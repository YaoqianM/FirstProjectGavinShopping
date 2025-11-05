package com.bfs.hibernateprojectdemo.controller;

import com.bfs.hibernateprojectdemo.domain.Product;
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
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(defaultValue = "false") boolean admin) {
        try {
            return ResponseEntity.ok(homePageService.getAvailableProducts(admin));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductDetail(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "false") boolean admin) {
        try {
            Product product = homePageService.getProductDetail(productId, admin);
            if (product == null) {
                return ResponseEntity.notFound().build();
            }
            String etag = String.format("W/\"product-%d-v%d\"", product.getProductId(),
                    product.getVersion() == null ? 0 : product.getVersion());
            return ResponseEntity.ok().eTag(etag).body(product);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasRole('USER')")
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
                return ResponseEntity.notFound().build();
            }

            // Concurrency control: If-Match against version ETag
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

            // Persist changes
            p.setName(patched.getName());
            p.setDescription(patched.getDescription());
            p.setRetailPrice(patched.getRetailPrice());
            p.setWholesalePrice(patched.getWholesalePrice());
            p.setQuantity(patched.getQuantity());

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
            s.save(p);
            tx.commit();
            return ResponseEntity.status(HttpStatus.CREATED).body(p);
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