package com.bfs.hibernateprojectdemo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    @GetMapping("/products/all")
    public String getWatchlist() {
        // user list watchlist
        return "Watchlist products";
    }

    @PostMapping("/product/{id}")
    public String addToWatchlist(@PathVariable Long id) {
        // user add to watchlist
        return "Added to watchlist";
    }

    @DeleteMapping("/product/{id}")
    public String removeFromWatchlist(@PathVariable Long id) {
        // user remove from watchlist
        return "Removed from watchlist";
    }

}
