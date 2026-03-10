package com.curator.app;

import com.curator.config.FirebaseConfig;
import com.curator.services.ArtProvider;
import com.curator.services.AuthService;
import com.curator.services.HeistReportRepository;
import com.curator.services.PuzzleProvider;
import com.curator.services.StolenArtRepository;
import com.curator.services.UserProfileRepository;
import com.curator.services.impl.ChicagoArtInstituteService;
import com.curator.services.impl.DisabledAuthService;
import com.curator.services.impl.FirebaseAuthService;
import com.curator.services.impl.FirestoreReportRepository;
import com.curator.services.impl.FirestoreStolenArtRepository;
import com.curator.services.impl.FirestoreUserProfileRepository;
import com.curator.services.impl.HeartApiService;
import com.curator.services.impl.NoOpHeistReportRepository;
import com.curator.services.impl.NoOpStolenArtRepository;
import com.curator.services.impl.NoOpUserProfileRepository;

// Central wiring keeps dependencies in one place (low coupling for the game layer).
public final class ServiceRegistry {

    private final ArtProvider artProvider;
    private final PuzzleProvider puzzleProvider;
    private final AuthService authService;
    private final HeistReportRepository reportRepository;
    private final StolenArtRepository stolenArtRepository;
    private final UserProfileRepository userProfileRepository;

    private ServiceRegistry(ArtProvider artProvider,
                            PuzzleProvider puzzleProvider,
                            AuthService authService,
                            HeistReportRepository reportRepository,
                            StolenArtRepository stolenArtRepository,
                            UserProfileRepository userProfileRepository) {
        this.artProvider = artProvider;
        this.puzzleProvider = puzzleProvider;
        this.authService = authService;
        this.reportRepository = reportRepository;
        this.stolenArtRepository = stolenArtRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public static ServiceRegistry createDefault() {
        ArtProvider artProvider = new ChicagoArtInstituteService();
        PuzzleProvider puzzleProvider = new HeartApiService();

        try {
            FirebaseConfig config = FirebaseConfig.load();
            AuthService authService = new FirebaseAuthService(config);
            HeistReportRepository reportRepository = new FirestoreReportRepository(config);
            StolenArtRepository stolenArtRepository = new FirestoreStolenArtRepository(config);
            UserProfileRepository userProfileRepository = new FirestoreUserProfileRepository(config);
            return new ServiceRegistry(artProvider, puzzleProvider, authService, reportRepository,
                    stolenArtRepository, userProfileRepository);
        } catch (Exception e) {
            AuthService authFallback = new DisabledAuthService(e.getMessage());
            HeistReportRepository reportFallback = new NoOpHeistReportRepository(e.getMessage());
            StolenArtRepository stolenFallback = new NoOpStolenArtRepository(e.getMessage());
            UserProfileRepository profileFallback = new NoOpUserProfileRepository(e.getMessage());
            return new ServiceRegistry(artProvider, puzzleProvider, authFallback, reportFallback,
                    stolenFallback, profileFallback);
        }
    }

    public ArtProvider artProvider() {
        return artProvider;
    }

    public PuzzleProvider puzzleProvider() {
        return puzzleProvider;
    }

    public AuthService authService() {
        return authService;
    }

    public HeistReportRepository reportRepository() {
        return reportRepository;
    }

    public StolenArtRepository stolenArtRepository() {
        return stolenArtRepository;
    }

    public UserProfileRepository userProfileRepository() {
        return userProfileRepository;
    }
}
