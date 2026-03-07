package gtc.dcc.put0.core.view;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.app.AlertDialog;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import gtc.dcc.put0.core.utils.GameMessageHelper;
import gtc.dcc.put0.core.utils.CoreLogger;
import gtc.dcc.put0.core.network.bluetooth.BluetoothHelper;
import gtc.dcc.put0.core.network.bluetooth.BluetoothHostService;
import gtc.dcc.put0.core.network.bluetooth.BluetoothClientService;

import gtc.dcc.put0.core.data.model.GameStatus;
import gtc.dcc.put0.core.data.model.MatchMode;
import gtc.dcc.put0.core.viewmodel.GameViewModel;
import gtc.dcc.put0.core.network.bluetooth.BluetoothMatchManager;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;
import gtc.dcc.put0.core.adapter.PlayerListAdapter;
import gtc.dcc.put0.databinding.ActivityLobbyBinding;

public class LobbyActivity extends AppCompatActivity {

    private ActivityLobbyBinding binding;
    private GameViewModel viewModel;
    private MatchMode currentMode;
    private String gameId;

    // Bluetooth variables
    private BluetoothHelper bluetoothHelper;
    private boolean isBluetoothHost = false;
    private boolean isBluetoothClient = false;
    private int deckSize = 52;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private PlayerListAdapter playerListAdapter;

    // Hold references to BT services so we can clear listeners in onDestroy()
    // This is the 'belt and suspenders' defense alongside WeakReference in the
    // services
    private BluetoothHostService hostService;
    private BluetoothClientService clientService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        initializeViewModel();

        bluetoothHelper = new BluetoothHelper(this);

        handleIntentData();
        setupUI();
        // Note: setupObservers() is called AFTER the MatchManager is wired
        // (inside startBluetoothHost / connectToBluetoothHost for BT modes,
        // or right here for non-BT modes).
        if (currentMode != MatchMode.BLUETOOTH_OFFLINE) {
            setupObservers();
        }
        startLoadingProcess();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void startLoadingProcess() {
        // For Bluetooth mode, skip the artificial delay — the lobby IS the waiting room
        if (currentMode == MatchMode.BLUETOOTH_OFFLINE) {
            binding.clLoadingOverlay.setVisibility(View.GONE);
            return;
        }
        // Online mode: show loading briefly until state arrives
        binding.clLoadingOverlay.setVisibility(View.VISIBLE);
        binding.clLoadingOverlay.postDelayed(() -> binding.clLoadingOverlay.setVisibility(View.GONE), 3500);
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
    }

    private void handleIntentData() {
        if (getIntent().hasExtra("GAME_ID")) {
            gameId = getIntent().getStringExtra("GAME_ID");
            // Trigger viewModel to load/observe this game
            // viewModel.joinRoom(gameId, "CurrentPlayer"); // Assuming already joined or
            // created?
            // If created, we might already have data in Repository, but good to
            // refresh/subscribe
        }

        if (getIntent().hasExtra("MATCH_MODE")) {
            String modeStr = getIntent().getStringExtra("MATCH_MODE");
            if ("BLUETOOTH_HOST".equals(modeStr)) {
                currentMode = MatchMode.BLUETOOTH_OFFLINE;
                isBluetoothHost = true;
                deckSize = getIntent().getIntExtra("DECK_SIZE", 52);
            } else if ("BLUETOOTH_CLIENT".equals(modeStr)) {
                currentMode = MatchMode.BLUETOOTH_OFFLINE;
                isBluetoothClient = true;
            } else {
                currentMode = (MatchMode) getIntent().getSerializableExtra("MATCH_MODE");
            }
        } else {
            currentMode = MatchMode.SOLO_VS_BOT; // Default fallback
        }
    }

    private void setupUI() {
        binding.tvGameCode
                .setText("CODE: " + (gameId != null ? gameId.substring(0, Math.min(gameId.length(), 6)) : "WAITING"));
        binding.tvModeInfo.setText("Mode: " + currentMode.name());

        switch (currentMode) {
            case SOLO_VS_BOT:
                binding.btnStartGame.setVisibility(View.VISIBLE);
                binding.btnShareCode.setVisibility(View.GONE);
                binding.tvPlayerCount.setVisibility(View.GONE);
                binding.rvLobbyPlayers.setVisibility(View.GONE); // Hide player list in solo

                // Adjust constraints or weight if needed, but for now just hiding is enough
                // to simplify the view as requested.
                binding.llInfoContainer
                        .setLayoutParams(new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                                0, 0));
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.llInfoContainer
                        .getLayoutParams();
                lp.matchConstraintPercentWidth = 1.0f; // Full width for info
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToBottom = binding.tvLobbyTitle.getId();
                lp.bottomToTop = binding.llActionButtons.getId();
                binding.llInfoContainer.setLayoutParams(lp);
                break;
            case SOLO_VS_AMIGO:
                binding.btnStartGame.setEnabled(false); // Enable only when 2 players
                binding.btnStartGame.setText("WAITING FOR PLAYER...");
                binding.btnShareCode.setVisibility(View.VISIBLE);
                break;
            case SOLO_VS_AMIGOS:
                binding.btnStartGame.setVisibility(View.VISIBLE);
                binding.btnShareCode.setVisibility(View.VISIBLE);
                break;
            case BLUETOOTH_OFFLINE:
                binding.tvGameCode.setText("BLUETOOTH MATCH");
                binding.btnShareCode.setVisibility(View.GONE);
                if (isBluetoothHost) {
                    binding.btnStartGame.setEnabled(false); // Wait for connection
                    binding.btnStartGame.setText("ESPERANDO JUGADOR...");
                    startBluetoothHost();
                } else if (isBluetoothClient) {
                    binding.btnStartGame.setVisibility(View.GONE);
                    binding.tvLobbyTitle.setText("UNIRSE A PARTIDA");
                    // Show dialog to pick device, then connect
                    showBluetoothDevicesDialog();
                }
                break;
        }

        binding.btnStartGame.setOnClickListener(v -> {
            if (currentMode == MatchMode.BLUETOOTH_OFFLINE) {
                if (isBluetoothHost) {
                    BluetoothMatchManager.getInstance().startGame();
                }
            } else {
                viewModel.startGame(gameId);
            }
        });

        binding.btnShareCode.setOnClickListener(v -> {
            // Share logic
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Join my PUT0 game! Code: " + gameId);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        });

        // Setup RecyclerView for players
        binding.rvLobbyPlayers.setLayoutManager(new LinearLayoutManager(this));
        playerListAdapter = new PlayerListAdapter(this, new java.util.ArrayList<>());
        binding.rvLobbyPlayers.setAdapter(playerListAdapter);

        // For Bluetooth modes, observers are set up after the MatchManager is wired.
        // Nothing to do here.
    }

    private void startBluetoothHost() {
        if (!bluetoothHelper.isBluetoothSupported()) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!BluetoothHelper.hasAllPermissions(this)) {
            BluetoothHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS);
            return; // startBluetoothHost() will be called again in onRequestPermissionsResult
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            // Request the system dialog to enable BT — more reliable than just a toast
            android.content.Intent enableBtIntent = new android.content.Intent(
                    android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1002);
            return;
        }

        String hostId = SharedPreferenceManager.getString("user_id", "host123");
        String hostName = SharedPreferenceManager.getString("user_name", "Host");

        hostService = new BluetoothHostService(bluetoothHelper.getAdapter(),
                BluetoothMatchManager.getInstance());

        BluetoothMatchManager.getInstance().initAsHost(hostService, hostId, hostName);
        viewModel.setMatchManager(BluetoothMatchManager.getInstance());

        // IMPORTANT: wire observers NOW, after the MatchManager is set,
        // so we observe BluetoothMatchManager's LiveData, not GameRepository's.
        setupObservers();

        hostService.start(hostId, hostName, deckSize);
        Toast.makeText(this, "Esperando conexión Bluetooth...", Toast.LENGTH_SHORT).show();
    }

    private void showBluetoothDevicesDialog() {
        if (!bluetoothHelper.isBluetoothSupported()) {
            Toast.makeText(this, "Este dispositivo no soporta Bluetooth.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!BluetoothHelper.hasAllPermissions(this)) {
            BluetoothHelper.requestPermissions(this, REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }

        if (!bluetoothHelper.isBluetoothEnabled()) {
            android.content.Intent enableBtIntent = new android.content.Intent(
                    android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1002);
            return;
        }

        java.util.List<android.bluetooth.BluetoothDevice> pairedDevices = bluetoothHelper.getPairedDevices(this);
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this,
                    "No hay dispositivos emparejados. Empareja el anfitrión primero en Ajustes > Bluetooth.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String[] deviceNames = new String[pairedDevices.size()];
        for (int i = 0; i < pairedDevices.size(); i++) {
            deviceNames[i] = pairedDevices.get(i).getName();
            if (deviceNames[i] == null)
                deviceNames[i] = "Dispositivo Desconocido";
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecciona el Anfitrión")
                .setItems(deviceNames, (dialog, which) -> {
                    android.bluetooth.BluetoothDevice selectedDevice = pairedDevices.get(which);
                    connectToBluetoothHost(selectedDevice);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void connectToBluetoothHost(android.bluetooth.BluetoothDevice device) {
        String clientId = SharedPreferenceManager.getString("user_id", "client123");
        String clientName = SharedPreferenceManager.getString("user_name", "Client");

        clientService = new BluetoothClientService(bluetoothHelper.getAdapter(),
                BluetoothMatchManager.getInstance());

        BluetoothMatchManager.getInstance().initAsClient(
                bluetoothHelper.getAdapter(), clientService, clientId, clientName);
        viewModel.setMatchManager(BluetoothMatchManager.getInstance());

        // IMPORTANT: wire observers NOW, after the MatchManager is set.
        setupObservers();

        try {
            String deviceName = device.getName() != null ? device.getName() : "Host";
            Toast.makeText(this, "Conectando a " + deviceName + "...", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Conectando...", Toast.LENGTH_SHORT).show();
        }
        clientService.connect(device);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Disconnect Activity's reference to services.
        // DO NOT call clearListener() here, as the listener is now the Singleton
        // BluetoothMatchManager!
        if (hostService != null) {
            hostService = null;
        }
        if (clientService != null) {
            clientService = null;
        }
        CoreLogger.d("PPUT0: [LOBBY] onDestroy.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (isBluetoothHost) {
                    startBluetoothHost();
                } else if (isBluetoothClient) {
                    showBluetoothDevicesDialog();
                }
            } else {
                Toast.makeText(this, "Permisos de Bluetooth necesarios para esta función.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupObservers() {
        viewModel.getGameState().observe(this, state -> {
            if (state == null)
                return;

            CoreLogger.d("PPUT0: [LOBBY] GameState received — status=" + state.getStatus()
                    + ", players=" + (state.getPlayers() != null ? state.getPlayers().size() : 0));

            // Always update player count and list
            int playerCount = state.getPlayers() != null ? state.getPlayers().size() : 0;
            binding.tvPlayerCount.setText("Jugadores: " + playerCount);

            if (playerListAdapter != null && state.getPlayers() != null) {
                playerListAdapter.updateData(state.getPlayers());
            }

            // Online mode: enable start when 2 players join
            if (currentMode == MatchMode.SOLO_VS_AMIGO && playerCount >= 2) {
                binding.btnStartGame.setEnabled(true);
                binding.btnStartGame.setText("START GAME");
            }

            // Bluetooth Host: enable start when a client connects (total 2 players)
            if (currentMode == MatchMode.BLUETOOTH_OFFLINE && isBluetoothHost && playerCount >= 2) {
                binding.btnStartGame.setEnabled(true);
                binding.btnStartGame.setText("INICIAR PARTIDA");
            }

            // ⚡ KEY: Navigate to game when status becomes PLAYING (both host & client)
            if (state.getStatus() == GameStatus.PLAYING) {
                CoreLogger.i("PPUT0: [LOBBY] Status is PLAYING — navigating to GameActivity");
                // Hide loading overlay if it was showing
                binding.clLoadingOverlay.setVisibility(View.GONE);

                Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                intent.putExtra("GAME_ID", state.getGameId());
                if (currentMode == MatchMode.BLUETOOTH_OFFLINE) {
                    intent.putExtra("IS_BLUETOOTH", true);
                    intent.putExtra("IS_HOST", isBluetoothHost);
                }
                // Use FLAG_ACTIVITY_CLEAR_TOP so the back button doesn't return to lobby
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                CoreLogger.w("PPUT0: [LOBBY] Error received: " + error);
                // Hide loading if there's an error
                binding.clLoadingOverlay.setVisibility(View.GONE);
                GameMessageHelper.showMessage(binding.getRoot(), error, GameMessageHelper.MessageType.NEUTRAL);
            }
        });
    }
}
