package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.dialogOnUiThread;
import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;
import static net.kdt.pojavlaunch.Tools.hasOnlineProfile;
import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.runOnUiThread;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Botões originais (agora ocultos, mas usados para a lógica)
        Button mDiscordButton = view.findViewById(R.id.discord_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);

        // Novos botões do Lobby
        ImageButton mLobbyDiscordButton = view.findViewById(R.id.lobby_discord_button);
        ImageButton mLobbyControlsButton = view.findViewById(R.id.lobby_controls_button);
        ImageButton mLobbySettingsButton = view.findViewById(R.id.lobby_settings_button);

        // Botões principais que permanecem visíveis
        Button mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        // Lógica dos botões originais
        View.OnClickListener discordAction = v -> Tools.openURL(requireActivity(), "https://discord.gg/e4QS6BMVUf");
        View.OnClickListener controlsAction = v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class));
        View.OnClickListener profileEditorAction = v -> mVersionSpinner.openProfileEditor(requireActivity());
        View.OnClickListener mainSettingsAction = v -> Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null);

        // Aplicar ações aos botões originais (para manter a funcionalidade caso o layout antigo seja usado)
        mDiscordButton.setOnClickListener(discordAction);
        mCustomControlButton.setOnClickListener(controlsAction);
        mEditProfileButton.setOnClickListener(profileEditorAction);

        // NERVIUM: Conectar os novos botões do lobby às ações corretas
        mLobbyDiscordButton.setOnClickListener(discordAction);
        mLobbyControlsButton.setOnClickListener(controlsAction);
        mLobbySettingsButton.setOnClickListener(mainSettingsAction); // AÇÃO CORRIGIDA

        // Ações dos outros botões
        mPlayButton.setOnClickListener(v -> {
            if (Tools.hasMods("sodium") && !(LauncherPreferences.DEFAULT_PREF.getBoolean("sodium_override", false))) {
                AlertDialog sodiumWarningDialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.sodium_warning_title)
                        .setMessage(R.string.sodium_warning_message)
                        .setNeutralButton(R.string.delete_sodium, (d,w)-> {
                            Tools.deleteSodiumMods();
                            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                        })
                        .create();
                sodiumWarningDialog.show();
            } else ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });

        // Botões que não estão mais visíveis no lobby, mas cujas referências precisam existir
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        if (hasOnlineProfile()) {
            mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
            mInstallJarButton.setOnLongClickListener(v -> {
                runInstallerWithConfirmation(true);
                return true;
            });
        } else mInstallJarButton.setOnClickListener(v -> hasNoOnlineProfileDialog(requireActivity()));
        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));
        mOpenDirectoryButton.setOnClickListener((v)-> {
            if (Tools.isDemoProfile(v.getContext())){ 
                hasNoOnlineProfileDialog(getActivity(), getString(R.string.demo_unsupported), getString(R.string.change_account));
            } else if (!hasOnlineProfile()) { 
                hasNoOnlineProfileDialog(requireActivity());
            } else openPath(v.getContext(), getCurrentProfileDirectory(), false);

        });
        mNewsButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if(!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if(profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
