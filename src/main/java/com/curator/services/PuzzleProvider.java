package com.curator.services;

import com.curator.domain.HeartPuzzle;
import java.util.concurrent.CompletableFuture;

// Interface isolates the puzzle mechanic from a specific Heart API implementation.
public interface PuzzleProvider {
    CompletableFuture<HeartPuzzle> fetchPuzzle();

    boolean isCorrect(HeartPuzzle puzzle, String userInput);
}
