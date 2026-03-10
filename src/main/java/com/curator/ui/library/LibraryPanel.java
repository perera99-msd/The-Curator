package com.curator.ui.library;

import com.curator.domain.AuthSession;
import com.curator.domain.StolenArtEntry;
import com.curator.services.StolenArtRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

// Cloud-backed library view for a single user's stolen artworks.
public class LibraryPanel extends VBox {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final StolenArtRepository repository;
    private final AuthSession session;

    private final Text summary = new Text();
    private final Text status = new Text();
    private final ListView<StolenArtEntry> list = new ListView<>();

    public LibraryPanel(StolenArtRepository repository, AuthSession session) {
        this.repository = repository;
        this.session = session;

        summary.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 22));
        summary.setFill(Color.rgb(246, 218, 154));

        status.setFont(Font.font("Bodoni MT", FontWeight.SEMI_BOLD, 18));
        status.setFill(Color.rgb(133, 225, 174));

        list.setPrefSize(760, 360);
        list.setStyle("-fx-background-color: rgba(4,10,18,0.62); -fx-control-inner-background: rgba(4,10,18,0.62);");
        list.setPlaceholder(new Text("No stolen artworks in the cloud vault yet."));
        list.setCellFactory(view -> new VaultCell());

        setSpacing(14);
        setPadding(new Insets(8, 12, 8, 12));
        getChildren().addAll(summary, list, status);

        if (session == null) {
            status.setText("Login required to access your cloud vault.");
            status.setFill(Color.rgb(255, 140, 140));
            list.setDisable(true);
        } else {
            refresh();
        }
    }

    public void refresh() {
        status.setText("Syncing vault from Firestore...");
        status.setFill(Color.rgb(133, 225, 174));
        repository.fetchStolenArt(session).thenAccept(entries -> Platform.runLater(() -> {
            updateList(entries);
            status.setText("Vault synced.");
            status.setFill(Color.rgb(125, 231, 183));
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                status.setText("Vault sync failed.");
                status.setFill(Color.rgb(255, 140, 140));
            });
            return null;
        });
    }

    private void updateList(List<StolenArtEntry> entries) {
        list.getItems().setAll(entries);
        int totalValue = entries.stream().mapToInt(entry -> entry.record().value()).sum();
        summary.setText("Recovered Pieces: " + entries.size() + "   |   Vault Value: $" + totalValue + "M");
    }

    private class VaultCell extends ListCell<StolenArtEntry> {
        @Override
        protected void updateItem(StolenArtEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            var imageView = new ImageView();
            imageView.setFitHeight(64);
            imageView.setFitWidth(64);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            String imageUrl = entry.record().imageUrl();
            Runnable setPlaceholder = () -> Platform.runLater(() -> {
                try {
                    imageView.setImage(new Image("https://picsum.photos/seed/vault" + entry.hashCode() + "/64/64", 64, 64, true, true));
                } catch (Exception ignored) {}
            });

            if (imageUrl != null && !imageUrl.isBlank()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .GET()
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                        .thenAccept(response -> {
                            if (response.statusCode() == 200) {
                                Platform.runLater(() -> {
                                    try {
                                        Image img = new Image(new ByteArrayInputStream(response.body()), 64, 64, true, true);
                                        imageView.setImage(img);
                                    } catch (Exception e) {
                                        setPlaceholder.run();
                                    }
                                });
                            } else {
                                setPlaceholder.run();
                            }
                        }).exceptionally(ex -> {
                            setPlaceholder.run();
                            return null;
                        });
            } else {
                setPlaceholder.run();
            }

            // Create a stylish frame for the artwork
            var frame = new javafx.scene.layout.StackPane(imageView);
            frame.setMinSize(72, 72);
            frame.setMaxSize(72, 72);
            frame.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #d4af37; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-radius: 4;");

            var title = new Text(entry.record().title());
            title.setFont(Font.font("Cinzel", FontWeight.SEMI_BOLD, 18));
            title.setFill(Color.rgb(232, 216, 184));

            var artist = new Text(entry.record().artist());
            artist.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 16));
            artist.setFill(Color.rgb(182, 214, 250, 0.92));

            var meta = new Text(entry.record().stolenAt() + "  |  " + entry.record().mode()
                    + "  |  $" + entry.record().value() + "M");
            meta.setFont(Font.font("Bodoni MT", FontWeight.NORMAL, 14));
            meta.setFill(Color.rgb(168, 198, 232, 0.86));

            var textBox = new VBox(4, title, artist, meta);
            textBox.setAlignment(Pos.CENTER_LEFT);

            var spacer = new Region();
            spacer.setMinWidth(24);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            var delete = new Button("Delete");
            delete.setStyle("-fx-background-color: rgba(40,12,16,0.85); -fx-text-fill: #f3d39a;"
                    + "-fx-border-color: rgba(255,150,120,0.65); -fx-border-radius: 8; -fx-background-radius: 8;");
            delete.setOnAction(e -> {
                delete.setDisable(true);
                repository.deleteStolenArt(session, entry.documentId())
                        .thenRun(() -> Platform.runLater(LibraryPanel.this::refresh))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> delete.setDisable(false));
                            return null;
                        });
            });

            var row = new HBox(14, frame, textBox, spacer, delete);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));
            setGraphic(row);
        }
    }
}
