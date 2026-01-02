import SwiftUI
import LiquidGlass

/// Smart Albums screen displaying auto-generated and custom albums in a grid layout.
///
/// Features:
/// - Liquid Glass design with blur effects
/// - Smart Grid layout with adaptive columns
/// - Animated album cards with spring animations
/// - Quick actions (share, delete, edit)
/// - Album badges for auto-generated albums
/// - Photo count indicators
/// - Gradient overlays for text readability
struct SmartAlbumsView: View {
    // MARK: - Properties
    @State private var albums: [Album]
    @State private var selectedAlbum: Album?
    @State private var showMenu: Bool = false
    @State private var showShareSheet: Bool = false
    @State private var showDeleteAlert: Bool = false
    @State private var albumToDelete: Album?

    let onAlbumClick: (Album) -> Void
    let onAlbumShare: (Album) -> Void
    let onAlbumDelete: (Album) -> Void
    let onAlbumEdit: (Album) -> Void

    // MARK: - Initialization
    init(
        albums: [Album],
        onAlbumClick: @escaping (Album) -> Void,
        onAlbumShare: @escaping (Album) -> Void = { _ in },
        onAlbumDelete: @escaping (Album) -> Void = { _ in },
        onAlbumEdit: @escaping (Album) -> Void = { _ in }
    ) {
        self._albums = State(initialValue: albums)
        self.onAlbumClick = onAlbumClick
        self.onAlbumShare = onAlbumShare
        self.onAlbumDelete = onAlbumDelete
        self.onAlbumEdit = onAlbumEdit
    }

    // MARK: - Body
    var body: some View {
        NavigationStack {
            ZStack {
                // Background gradient
                LinearGradient(
                    colors: [
                        Color.blue.opacity(0.1),
                        Color.purple.opacity(0.1)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                if albums.isEmpty {
                    // Empty state
                    EmptyAlbumsStateView()
                } else {
                    // Albums grid
                    ScrollView {
                        LazyVGrid(
                            columns: [
                                GridItem(.adaptive(minimum: 160), spacing: 12)
                            ],
                            spacing: 12
                        ) {
                            ForEach(albums, id: \.id) { album in
                                AlbumCard(
                                    album: album,
                                    onClick: {
                                        withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                            selectedAlbum = album
                                            onAlbumClick(album)
                                        }
                                    },
                                    onShare: {
                                        withAnimation {
                                            selectedAlbum = album
                                            showShareSheet = true
                                            onAlbumShare(album)
                                        }
                                    },
                                    onDelete: {
                                        withAnimation {
                                            albumToDelete = album
                                            showDeleteAlert = true
                                        }
                                    },
                                    onEdit: {
                                        onAlbumEdit(album)
                                    }
                                )
                                .transition(.scale.combined(with: .opacity))
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }
                }
            }
            .navigationTitle("Albums")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 8) {
                        Button(action: {
                            // TODO: Create new album
                        }) {
                            Image(systemName: "plus")
                                .font(.title3)
                                .foregroundStyle(.primary)
                        }

                        Button(action: {
                            // TODO: Filter albums
                        }) {
                            Image(systemName: "line.3.horizontal.decrease.circle")
                                .font(.title3)
                                .foregroundStyle(.primary)
                        }
                    }
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let album = selectedAlbum {
                    ShareSheet(items: [album.name])
                }
            }
            .alert("Delete Album", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) {
                    albumToDelete = nil
                }
                Button("Delete", role: .destructive) {
                    if let album = albumToDelete {
                        onAlbumDelete(album)
                        albums.removeAll { $0.id == album.id }
                    }
                    albumToDelete = nil
                }
            } message: {
                Text("Are you sure you want to delete this album? This action cannot be undone.")
            }
        }
    }
}

// MARK: - Album Card
struct AlbumCard: View {
    let album: Album
    let onClick: () -> Void
    let onShare: () -> Void
    let onDelete: () -> Void
    let onEdit: () -> Void

    @State private var isPressed: Bool = false
    @State private var showMenu: Bool = false

    var body: some View {
        LiquidGlassCard {
            ZStack(alignment: .topLeading) {
                // Cover photo placeholder
                RoundedRectangle(cornerRadius: 16)
                    .fill(
                        LinearGradient(
                            colors: [
                                Color.blue.opacity(0.3),
                                Color.purple.opacity(0.3)
                            ]
                        )
                    )

                // Auto-generated badge
                if album.isAutoGenerated {
                    Image(systemName: "sparkles")
                        .font(.caption)
                        .foregroundStyle(.white)
                        .padding(8)
                        .background(Circle().fill(Color.accentColor))
                        .padding(8)
                }

                // Photo count
                Text("\(album.photoCount())")
                    .font(.caption)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        Capsule()
                            .fill(Color.black.opacity(0.5))
                    )
                    .padding(8)
                    .frame(maxWidth: .infinity, alignment: .trailing)

                // Album info (bottom)
                VStack(alignment: .leading, spacing: 4) {
                    Spacer()

                    Text(album.name)
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .shadow(color: .black.opacity(0.3), radius: 2)
                        .padding(.horizontal, 12)
                        .padding(.bottom, 12)
                }
            }
            .frame(aspectRatio: 1)
            .overlay(
                // Gradient overlay
                LinearGradient(
                    colors: [
                        Color.clear,
                        Color.black.opacity(0.6)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
            )
            .overlay(
                // Quick action menu button
                Button(action: {
                    withAnimation {
                        showMenu.toggle()
                    }
                }) {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                        .foregroundStyle(.white)
                        .padding(8)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(8)
            )
        }
        .scaleEffect(isPressed ? 0.95 : 1.0)
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: isPressed)
        .onTapGesture {
            withAnimation {
                isPressed = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation {
                    isPressed = false
                }
                onClick()
            }
        }
        .confirmationDialog("Album Options", isPresented: $showMenu) {
            Button("Share") {
                onShare()
            }
            Button("Edit") {
                onEdit()
            }
            Button("Delete", role: .destructive) {
                onDelete()
            }
        } message: {
            Text("Choose an action for this album")
        }
    }
}

// MARK: - Empty State
struct EmptyAlbumsStateView: View {
    var body: some View {
        VStack(spacing: 24) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 120))
                .foregroundStyle(.secondary.opacity(0.3))

            Text("No albums yet")
                .font(.title)
                .foregroundStyle(.primary.opacity(0.6))

            Text("Photos will be automatically organized into albums")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Preview
#Preview("Albums List") {
    SmartAlbumsView(
        albums: [
            Album(
                id: "1",
                eventId: "event-1",
                name: "Summer Vacation 2025",
                coverPhotoId: "photo-1",
                photoIds: ["photo-1", "photo-2", "photo-3"],
                createdAt: "2025-01-01T00:00:00Z",
                isAutoGenerated: true
            ),
            Album(
                id: "2",
                eventId: "event-2",
                name: "Wedding Ceremony",
                coverPhotoId: "photo-4",
                photoIds: ["photo-4", "photo-5", "photo-6"],
                createdAt: "2025-01-02T00:00:00Z",
                isAutoGenerated: false
            )
        ],
        onAlbumClick: { album in
            print("Tapped album: \(album.name)")
        }
    )
}

#Preview("Empty State") {
    SmartAlbumsView(
        albums: [],
        onAlbumClick: { _ in }
    )
}
