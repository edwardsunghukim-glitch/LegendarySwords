# Install or update LegendarySwords

Use the compiled JAR and ZIPs from the **0.2.2** release. You do not need Maven on your computer just to install them on Minehut.

## Upload the plugin on Minehut

1. Stop your server in the Minehut dashboard.
2. Open **File Manager → plugins**. Remove the old LegendarySwords JAR, including any 0.2.0 or 0.2.1 version. Keep the `LegendarySwords` configuration folder.
3. Upload **`LegendarySwords-0.2.2.jar`** directly inside `plugins`. Do not put it inside the configuration folder, and keep only one LegendarySwords JAR.
4. Start the server. Check the console for a successful enable message. Use **Paper 1.21.11 and Java 25 with BetterModel 3.1**. Core combat without BetterModel can use Java 21. These are server requirements; uploading the compiled plugin does not require Java on your computer.
5. Join as an operator and run `/ls status`, then `/ls give all`. Newly given swords include the updated item attributes and models.

An existing configuration can keep your prior custom damage and cooldown values. New swords use their defaults when settings are missing. Compare with the new release's `src/main/resources/config.yml` if you want to reset to the supplied values.

**For the 0.2.2 update:** restarting updates unedited bundled BetterModel files from 0.2.0/0.2.1. If the console reports edited files preserved, back them up and manually replace those files using `LegendarySwords-BetterModel-models.zip` as described in [the animation guide](BETTERMODEL.md#install-or-upgrade-the-models). **Replace the old combined pack with `LegendarySwords-combined-resource-pack-0.2.2.zip`.** The new item selector keeps the hotbar sword visible while the animated body holds its own copy. The old combined pack lacks that selector, even though BetterModel's geometry/textures are unchanged.

## Install the new sword pack

For a personal test:

1. In Minecraft, open **Options → Resource Packs → Open Pack Folder**.
2. Copy the new **`LegendarySwords-combined-resource-pack-0.2.2.zip`** into that folder if using BetterModel, or the new **`LegendarySwords-resource-pack.zip`** for sword items alone. **Keep it zipped.**
3. Return to Minecraft, disable the older LegendarySwords pack, enable the new one, and click **Done**.

The updated combined pack gives all eight swords their item models and includes BetterModel body/slash assets from `build (12).zip`. If you changed other BetterModel models or configuration, generate a fresh BetterModel pack and combine it with the updated sword pack using [the animation guide](BETTERMODEL.md).

**Replace earlier combined packs when upgrading to 0.2.2.** Installing a new plugin JAR cannot update a resource pack already stored on your computer or hosted at an old URL. Install the updated ZIP and use its new SHA-1 for server delivery.

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

Use **F5** to see yourself in third person. Walk, sprint, and run `/ls pose` while holding each sword. Other players can see the third-person animations too. These do not replace your vanilla first-person arm swing.

If `/ls status` reports missing animations, confirm BetterModel's version, its enabled modules, the sixteen model files, and that your client accepted the new generated pack. See [BetterModel troubleshooting](BETTERMODEL.md#check-missing-animations).

## Configuration

The plugin creates `plugins/LegendarySwords/config.yml`. Use `/ls reload` after editing it. Model edits additionally require `/bm reload`; changes to generated client assets also require a regenerated, redistributed resource pack.

- `pvp-enabled: false` prevents abilities from damaging players by default.
- `cinematics.enabled: false` disables the cosmetic ultimate presentation. `/ls cinematics` is the individual player preference. Damage casts do not wait for that presentation.
- `glow-particles: true` enables particles around held swords.
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

Upload the resulting JAR to the server after removing the old JAR, then restart. Pack or generated BetterModel client-asset changes also require updated client packs and the correct final ZIP hash. Version 0.2.2 requires the updated combined pack for its item selectors; the merge can reuse BetterModel's `build (12).zip` assets while other models/configuration stay unchanged.
