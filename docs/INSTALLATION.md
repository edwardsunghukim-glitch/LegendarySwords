# Install or update LegendarySwords

Use **`LegendarySwords-0.2.3.jar`** with the existing **0.2.2 combined resource pack**. You do not need Maven on your computer just to install them on Minehut. The hotfix changes no resource-pack assets or model geometry.

## Upload the plugin on Minehut

1. Stop your server in the Minehut dashboard.
2. Open **File Manager → plugins**. Remove the old LegendarySwords JAR, including version 0.2.2. Keep the `LegendarySwords` configuration folder.
3. Upload **`LegendarySwords-0.2.3.jar`** directly inside `plugins`. Do not put it inside the configuration folder, and keep only one LegendarySwords JAR.
4. In `plugins/LegendarySwords/config.yml`, keep `bettermodel.enabled: true` and `bettermodel.player-body-animations: false`. A missing body-animation key uses the new false default; if it explicitly says true, change it to false before restarting.
5. Start the server. Check the console for a successful enable message. Use **Paper 1.21.11 and Java 25 with BetterModel 3.1**. Core combat without BetterModel can use Java 21. These are server requirements; uploading the compiled plugin does not require Java on your computer.
6. Join as an operator and run `/ls status`, then `/ls give all`. Confirm that body animations are paused while BetterModel remains available for slash models; ultimate presentation remains available.

An existing configuration can keep your prior custom damage and cooldown values. New swords use their defaults when settings are missing. Compare with the new release's `src/main/resources/config.yml` if you want to reset to the supplied values.

**For the 0.2.3 hotfix:** the reported Minecraft 1.21.11 client disconnected while decoding an equipment packet with 557 unread bytes on Paper 1.21.11 and BetterModel `3.1-SNAPSHOT-526`. The hotfix copies item metadata before replacing inventory items, gives BetterModel cached server-thread item snapshots, and temporarily pauses player body animations. The exact packet cause has not been proven, and no live-client test has been performed. Custom standing/walking/running, pose, and player action body animations are paused; textures, swords, combat effects, abilities, slash models, and ultimate presentation remain available.

**Upgrading from 0.2.2:** keep your installed 0.2.2 combined pack. **Upgrading from 0.2.0 or 0.2.1:** install `LegendarySwords-combined-resource-pack-0.2.2.zip`, because earlier packs lack its item selector. Restarting updates known unedited old BetterModel player files to the bundled 0.2.2 clips. Edited files are preserved; back them up before manual replacement using `LegendarySwords-BetterModel-models.zip` as described in [the animation guide](BETTERMODEL.md#install-or-upgrade-the-models).

## Install or retain the sword pack

For a personal test:

1. In Minecraft, open **Options → Resource Packs → Open Pack Folder**.
2. Use **`LegendarySwords-combined-resource-pack-0.2.2.zip`** if using BetterModel, or **`LegendarySwords-resource-pack.zip`** for sword items alone. **Keep it zipped.** If the 0.2.2 pack is already installed, keep it.
3. Return to Minecraft, disable obsolete LegendarySwords packs, enable the correct one, and click **Done**.

The 0.2.2 combined pack gives all eight swords their item models and includes BetterModel body/slash assets from `build (12).zip`. Version 0.2.3 reuses these bytes unchanged. If you changed other BetterModel models or configuration, generate a fresh BetterModel pack and combine it with the sword pack using [the animation guide](BETTERMODEL.md).

**Replace packs from before 0.2.2.** Installing a plugin JAR cannot update a resource pack already stored on your computer or hosted at an old URL. No pack or hash change is required when upgrading an existing 0.2.2 setup to the 0.2.3 hotfix.

For everyone joining the server:

1. Host the final ZIP at a public URL that directly returns the file over HTTPS.
2. Use that URL in Minehut's server resource-pack setting, or configure LegendarySwords' `resource-pack.url`.
3. Set the SHA-1 of that **exact final ZIP**. A merged pack needs the merged pack's hash, not the standalone sword pack's hash.
4. Save the settings, restart the server, join, and accept the pack prompt.

The merge script writes a `.sha1` file alongside its ZIP. In Windows PowerShell you can also calculate it:

```powershell
(Get-FileHash .\LegendarySwords-combined-resource-pack-0.2.2.zip -Algorithm SHA1).Hash.ToLower()
```

If using LegendarySwords' configuration:

```yaml
resource-pack:
  url: 'https://your-host.example/LegendarySwords-combined-resource-pack-0.2.2.zip'
  sha1: 'paste_the_40_character_sha1_here'
  required: false
  send-on-join: true
  join-delay-ticks: 60
```

Replace the example URL and hash. A blank URL disables this plugin's automatic pack delivery. If Minehut already sends the combined pack, keep this URL blank to avoid duplicate offers. BetterModel should also use the same combined pack or have its separate pack offer disabled; the setting names depend on its installed version.

## Try every sword

Run these in Minecraft as an operator:

```text
/ls give all
/ls status
/ls charge 100
```

Hold a sword and **right-click** for its ability. **Sneak + right-click** casts its ultimate. Before trying another ultimate, use `/ls charge 100`; if you want to repeat one immediately while testing, also use `/ls resetcooldowns`.

Use **F5** to check sword visibility and combat effects while holding each sword. Custom player standing, walking, running, pose, and action body animations are temporarily disabled by default; `/ls pose` cannot play a body pose in this mode. The ultimate presentation and attack effects remain available. The vanilla first-person arm swing remains standard.

The body-animation pause in `/ls status` is expected for 0.2.3. For missing slash models, confirm BetterModel's version, its enabled modules, the sixteen model files, and that your client accepted the combined pack. See [BetterModel troubleshooting](BETTERMODEL.md#check-missing-animations).

## Configuration

The plugin creates `plugins/LegendarySwords/config.yml`. Use `/ls reload` for ordinary damage/cooldown settings. Restart after changing the body-animation switch. Model edits additionally require `/bm reload`; changes to generated client assets also require a regenerated, redistributed resource pack.

- `pvp-enabled: false` prevents abilities from damaging players by default.
- `cinematics.enabled: false` disables the cosmetic ultimate presentation. `/ls cinematics` is the individual player preference. Damage casts do not wait for that presentation.
- `glow-particles: true` enables particles around held swords.
- `bettermodel.enabled: true` keeps BetterModel integration available for slash models.
- `bettermodel.player-body-animations: false` is the temporary 0.2.3 default, including for an old configuration with no such key. Only set it to true for a controlled test after gathering evidence for the equipment-packet issue, then restart and follow [the controlled animation checklist](TESTING.md#controlled-player-body-animation-test).
- `charge.per-melee-hit` and `charge.melee-kill-bonus` set charge gains.
- Each entry under `swords` sets damage, attack speed, and cooldowns. Damage uses health points: **2 points = 1 heart**.

Changing base attack damage or speed requires newly given items. Ability and ultimate settings are read from configuration. Check [server behavior and protections](TESTING.md) on a test world before public play.

## Put the source in your GitHub repository

Your cloned `LegendarySwords` folder contains a hidden `.git` folder. Leave it in place; it connects the folder to your GitHub repository.

1. Extract **`LegendarySwords-source.zip`** on your computer.
2. Open the extracted folder containing `pom.xml`, `README.md`, `src`, and `resource-pack`.
3. In GitHub Desktop, select **LegendarySwords → Show in Explorer**.
4. Copy the **contents** from step 2 into the repository folder. `pom.xml` should sit directly beside `.git`.
5. In GitHub Desktop, enter a summary, **Commit to main**, and **Push origin**.

Do not add server worlds, passwords, or the generated `target` folder. The source ZIP includes `.gitignore`.

## Build changes locally

Install a Java **25 or newer JDK** and Maven 3.9 or newer. Verify `java -version` and `mvn -version` both use the newer JDK, open a terminal in the project folder, and run:

```sh
mvn --batch-mode verify
python3 scripts/create_release.py
```

Upload the resulting JAR to the server after removing the old JAR, then restart. Pack or generated BetterModel client-asset changes also require updated client packs and the correct final ZIP hash. The 0.2.3 hotfix changes no client assets; keep the 0.2.2 combined pack while other models/configuration stay unchanged.
