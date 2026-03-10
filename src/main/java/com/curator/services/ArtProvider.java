package com.curator.services;

import com.curator.domain.ArtData;
import com.curator.domain.GameMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Interface (IArtProvider) keeps the game loosely coupled to the art data source.
public interface ArtProvider {
    CompletableFuture<List<ArtData>> fetchArtworks(GameMode mode);
}
