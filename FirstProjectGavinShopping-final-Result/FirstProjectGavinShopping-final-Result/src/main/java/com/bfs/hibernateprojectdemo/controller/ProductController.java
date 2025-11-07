package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Product;
import com.bfs.hibernateprojectdemo.dto.UserProductDto;
import com.bfs.hibernateprojectdemo.service.HomePageService;
import com.bfs.hibernateprojectdemo.service.ProductAnalyticsService;
import com.bfs.hibernateprojectdemo.util.PatchUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final SessionFactory sessionFactory;
    private final HomePageService homePageService;
    private final ProductAnalyticsService productAnalyticsService;
    private final ObjectMapper objectMapper;
    private final com.bfs.hibernateprojectdemo.service.AuditLogService auditLogService;

    public ProductController(SessionFactory sessionFactory,
                             HomePageService homePageService,
                             ProductAnalyticsService productAnalyticsService,
                             ObjectMapper objectMapper,
                             com.bfs.hibernateprojectdemo.service.AuditLogService auditLogService) {
        this.sessionFactory = sessionFactory;
        this.homePageService = homePageService;
        this.productAnalyticsService = productAnalyticsService;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

// Inside ProductController

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts(@RequestParam(defaultValue = "false") boolean admin) {
        try {
            List<Product> products = homePageService.getAvailableProducts(admin);
            if (products == null || products.isEmpty()) {
                return ResponseEntity.ok(new com.bfs.hibernateprojectdemo.dto.MessageResponse("No products available"));
            }
            if (admin) {
                return ResponseEntity.ok(products);
            } else {
                // Map to user-safe DTO (no wholesale, no version, no quantity)
                List<UserProductDto> dtos = new java.util.ArrayList<>();
                for (Product p : products) {
                    UserProductDto dto = new UserProductDto();
                    dto.setProductId(p.getProductId());
                    dto.setName(p.getName());
                    dto.setDescription(p.getDescription());
                    dto.setRetailPrice(p.getRetailPrice());
                    dtos.add(dto);
                }
                return ResponseEntity.ok(dtos);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching products");
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetail(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "false") boolean admin) {
        try {
            Product product = homePageService.getProductDetail(productId, admin);
            if (product == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new com.bfs.hibernateprojectdemo.dto.MessageResponse("Product not found"));
            }
            String etag = String.format("W/\"product-%d-v%d\"", product.getProductId(),
                    product.getVersion() == null ? 0 : product.getVersion());
            if (admin) {
                return ResponseEntity.ok().eTag(etag).body(product);
            } else {
                UserProductDto dto = new UserProductDto();
                dto.setProductId(product.getProductId());
                dto.setName(product.getName());
                dto.setDescription(product.getDescription());
                dto.setRetailPrice(product.getRetailPrice());
                return ResponseEntity.ok().eTag(etag).body(dto);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(path = "/{productId}", consumes = {"application/json-patch+json", "application/merge-patch+json", "application/json"})
    public ResponseEntity<?> updateProduct(@PathVariable Long productId,
                                           @RequestBody String patchBody,
                                           @RequestHeader(value = "Content-Type", required = false) String contentType,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           java.security.Principal principal) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            Product p = s.get(Product.class, productId);

            if (p == null) {
                tx.rollback();
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new com.bfs.hibernateprojectdemo.dto.MessageResponse("Product not found"));
            }

            // Concurrency control: If-Match against version ETag ;; Entity Tag;
            if (ifMatch != null && !ifMatch.isEmpty()) {
                String currentEtag = String.format("W/\"product-%d-v%d\"", p.getProductId(),
                        p.getVersion() == null ? 0 : p.getVersion());
                if (!ifMatch.equals(currentEtag)) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                            .body("ETag mismatch: expected " + currentEtag);
                }
            }

            // Apply patch using appropriate strategy
            Product patched;
            PatchUtils patchUtils = new PatchUtils(objectMapper);
            try {
                if (contentType != null && contentType.startsWith("application/json-patch+json")) {
                    JsonPatch patch = objectMapper.readValue(patchBody, JsonPatch.class);
                    patched = patchUtils.applyJsonPatch(patch, p, Product.class);
                } else if (contentType != null && contentType.startsWith("application/merge-patch+json")) {
                    JsonMergePatch patch = objectMapper.readValue(patchBody, JsonMergePatch.class);
                    patched = patchUtils.applyMergePatch(patch, p, Product.class);
                } else {
                    // Fallback: treat body as partial JSON and deep-merge
                    JsonNode node = objectMapper.readTree(patchBody);
                    JsonMergePatch patch = JsonMergePatch.fromJson(node);
                    patched = patchUtils.applyMergePatch(patch, p, Product.class);
                }
            } catch (Exception pe) {
                tx.rollback();
                return ResponseEntity.badRequest().body("Invalid patch: " + pe.getMessage());
            }

            // Validate patched resource
            String validationError = validateProduct(patched);
            if (validationError != null) {
                tx.rollback();
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(validationError);
            }

            // Compute change set for audit
            StringBuilder csb = new StringBuilder("{");
            boolean first = true;
            first = appendChange(csb, first, "name", p.getName(), patched.getName());
            first = appendChange(csb, first, "description", p.getDescription(), patched.getDescription());
            first = appendChange(csb, first, "retailPrice", p.getRetailPrice(), patched.getRetailPrice());
            first = appendChange(csb, first, "wholesalePrice", p.getWholesalePrice(), patched.getWholesalePrice());
            first = appendChange(csb, first, "quantity", p.getQuantity(), patched.getQuantity());
            csb.append("}");

            // If name changed to an existing product's name, merge-by-name
            if (patched.getName() != null && !patched.getName().equals(p.getName())) {
                Product sameName = s.createQuery(
                                "from Product x where x.name = :nm and x.productId <> :pid",
                                Product.class)
                        .setParameter("nm", patched.getName())
                        .setParameter("pid", p.getProductId())
                        .uniqueResult();
                if (sameName != null) {
                    // Merge fields: update description and prices from patched, accumulate quantity
                    int mergedQty = sameName.getQuantity() + patched.getQuantity();
                    sameName.setDescription(patched.getDescription());
                    sameName.setRetailPrice(patched.getRetailPrice());
                    sameName.setWholesalePrice(patched.getWholesalePrice());
                    sameName.setQuantity(mergedQty);
                    if (sameName.getQuantity() <= 0) {
                        s.delete(sameName);
                    } else {
                        s.update(sameName);
                    }
                    // Delete original product as it merged into sameName
                    s.delete(p);
                    tx.commit();
                    auditLogService.log("Product", sameName.getProductId(), null, csb.toString(),
                            p.getVersion() == null ? null : p.getVersion(),
                            sameName.getVersion() == null ? null : sameName.getVersion());
                    return ResponseEntity.ok(sameName);
                }
            }

            // Persist changes when not merging
            p.setName(patched.getName());
            p.setDescription(patched.getDescription());
            p.setRetailPrice(patched.getRetailPrice());
            p.setWholesalePrice(patched.getWholesalePrice());
            p.setQuantity(patched.getQuantity());
            // If quantity became non-positive, remove product entirely to enforce inventory rule
            if (p.getQuantity() <= 0) {
                s.delete(p);
                tx.commit();
                return ResponseEntity.ok("Product removed due to non-positive quantity");
            }

            s.update(p);
            tx.commit();

            Long uid = null;
            if (principal != null) {
                try {
                    // fetch user id by username
                    com.bfs.hibernateprojectdemo.domain.User dbUser = s.createQuery("from User where username = :un", com.bfs.hibernateprojectdemo.domain.User.class)
                            .setParameter("un", principal.getName())
                            .uniqueResult();
                    if (dbUser != null) uid = dbUser.getId();
                } catch (Exception ignored) {}
            }
            auditLogService.log("Product", p.getProductId(), uid, csb.toString(),
                    p.getVersion() == null ? null : p.getVersion(),
                    p.getVersion() == null ? null : p.getVersion());
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating product: " + e.getMessage());
        }
    }

    private String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number) return v.toString();
        return "\"" + v.toString().replace("\"", "\\\"") + "\"";
    }

    private boolean appendChange(StringBuilder csb, boolean first, String field, Object from, Object to) {
        if (!java.util.Objects.equals(from, to)) {
            if (!first) csb.append(",");
            csb.append("\"").append(field).append("\":{")
                    .append("\"from\":").append(toJsonValue(from)).append(",")
                    .append("\"to\":").append(toJsonValue(to)).append("}");
            return false;
        }
        return first;
    }

    private String validateProduct(Product p) {
        if (p.getName() == null || p.getName().trim().isEmpty()) {
            return "name must be non-empty";
        }
        if (p.getRetailPrice() < 0) {
            return "retailPrice must be >= 0";
        }
        if (p.getWholesalePrice() < 0) {
            return "wholesalePrice must be >= 0";
        }
        if (p.getQuantity() < 0) {
            return "quantity must be >= 0";
        }
        return null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product p) {
        try (Session s = sessionFactory.openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                // Reject creation when initial quantity is <= 0
                if (p.getQuantity() <= 0) {
                    tx.rollback();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Quantity must be greater than 0");
                }
                // Check for existing product by name ONLY (merge if same name)
                Product existing = s.createQuery(
                        "from Product p where p.name = :name",
                        Product.class)
                        .setParameter("name", p.getName())
                        .uniqueResult();

                if (existing != null) {
                    // Merge fields: update description and prices, accumulate quantity
                    int newQty = existing.getQuantity() + p.getQuantity();
                    existing.setDescription(p.getDescription());
                    existing.setRetailPrice(p.getRetailPrice());
                    existing.setWholesalePrice(p.getWholesalePrice());
                    existing.setQuantity(newQty);
                    if (existing.getQuantity() <= 0) {
                        // Remove product completely when resulting quantity is non-positive
                        s.delete(existing);
                        tx.commit();
                        return ResponseEntity.ok("Product removed due to non-positive quantity");
                    }
                    s.update(existing);
                    tx.commit();
                    return ResponseEntity.ok(existing);
                } else {
                    s.save(p);
                    tx.commit();
                    return ResponseEntity.status(HttpStatus.CREATED).body(p);
                }
            } catch (Exception ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating product: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/frequent/{n}")
    public ResponseEntity<?> getTopFrequent(@PathVariable int n) {
        try {
            if (n <= 0) {
                return ResponseEntity.badRequest().body("n must be >= 1");
            }
            return ResponseEntity.ok(productAnalyticsService.getTopFrequent(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/recent/{n}")
    public ResponseEntity<?> getTopRecent(@PathVariable int n) {
        try {
            if (n <= 0) {
                return ResponseEntity.badRequest().body("n must be >= 1");
            }
            return ResponseEntity.ok(productAnalyticsService.getTopRecent(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/profit/{n}")
    // this is most propular product; if it has been enter n; it means output the top n; such as top 3 product; instead of the third; 
    // same for most profitable product;
    public ResponseEntity<?> getTopProfit(@PathVariable int n) {
        try {
            if (n <= 0) {
                return ResponseEntity.badRequest().body("n must be >= 1");
            }
            return ResponseEntity.ok(productAnalyticsService.getTopProfit(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/popular/{n}")
        // this is most propular product; if it has been enter n; it means output the top n; such as top 3 product; instead of the third; 
    // same for most profitable product;
    public ResponseEntity<?> getTopPopular(@PathVariable int n) {
        try {
            if (n <= 0) {
                return ResponseEntity.badRequest().body("n must be >= 1");
            }
            return ResponseEntity.ok(productAnalyticsService.getTopPopular(n));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}