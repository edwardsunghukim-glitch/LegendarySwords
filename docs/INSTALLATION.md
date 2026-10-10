# Install LegendarySwords

Use the compiled JAR and resource pack from the release download. You do not need Maven on your computer just to install them on Minehut. Keep a backup of your server before installing new plugins.

## On Minehut

1. Stop your server in the Minehut dashboard.
2. Open the server's file manager and open `plugins`.
3. Upload `LegendarySwords-0.1.0.jar` there. The JAR belongs directly inside `plugins`, not inside a second `LegendarySwords` folder.
4. Start the server. The plugin generates its own `plugins/LegendarySwords/config.yml`.
5. Check the startup console for a successful enable message. If it says the plugin cannot load, check that the server is Paper 1.21.11 and is using Java 21.
6. Join as an operator and run `/ls status`, then `/ls give all`.

If BetterModel is already installed, keep it installed and follow [the animation guide](BETTERMODEL.md) to add the supplied assets. BetterModel needs its own generated resource pack for its models. Test both plugins after a full server restart.

## Install the resource pack

For a quick personal test:

1. Open Minecraft **Options → Resource Packs → Open Pack Folder**.
2. Copy `LegendarySwords-resource-pack.zip` into that folder.
3. Return to Minecraft, enable the pack, and press **Done**.

For everyone joining the server:

1. Upload `LegendarySwords-resource-pack.zip` to a host that serves the ZIP directly over HTTPS. A public GitHub release asset is one option.
2. Copy its direct download URL. A link to a GitHub page, your computer's file path, or `localhost` will not work for other players.
3. Compute the ZIP's SHA-1 hash. The build also provides a hash file alongside the ZIP when available. In Windows PowerShell:

   ```powershell
   (Get-FileHash .\LegendarySwords-resource-pack.zip -Algorithm SHA1).Hash.ToLower()
   ```

4. Edit `plugins/LegendarySwords/config.yml`, set `resource-pack.url` to your direct URL and `resource-pack.sha1` to the hash, or use Minehut's server resource pack settings. Save the configuration and restart the server. For example:

   ```yaml
   resource-pack:
     url: 'https://your-host.example/LegendarySwords-resource-pack.zip'
     sha1: 'paste_the_40_character_sha1_here'
     required: false
     send-on-join: true
     join-delay-ticks: 60
   ```

   Replace the example URL and hash with your real values. A blank URL disables the plugin's automatic pack download. If you use a merged BetterModel pack, its URL and SHA-1 must describe that final merged ZIP.
5. Join, accept the pack prompt, and check all three swords. When using the plugin's pack configuration, `/ls pack` requests it again.

When you replace the ZIP, update both the URL if it changed and the SHA-1. A stale hash can make Minecraft reject the pack. Read [the resource pack guide](../resource-pack/README.md) when combining the swords with BetterModel's generated pack.

## Useful configuration

The plugin creates `plugins/LegendarySwords/config.yml` on its first start. Use `/ls reload` after editing it, except when adding or changing BetterModel source models, which need BetterModel to regenerate its pack.

After changing a sword's base `attack-damage` or `attack-speed`, give fresh swords with `/ls give` to update their item attributes. Ability damage and cooldown changes take effect from configuration without replacing items.

- `pvp-enabled: false` prevents abilities from damaging players by default.
- `cinematics.enabled: false` disables all ultimate cameras server-wide. `/ls cinematics` is each player's own preference while cameras are enabled.
- `glow-particles: true` enables the particles around held custom swords.
- `charge.per-melee-hit` and `charge.melee-kill-bonus` control charge gains.
- Each entry under `swords` contains damage, attack speed, ability cooldown, and ultimate cooldown settings. Damage uses health points: **2 points = 1 heart**.

Always test damage and protection behavior in a test world before enabling PvP for players.

## Put the source in your GitHub repository

Your cloned `LegendarySwords` folder already contains a hidden `.git` folder. Leave `.git` in place; it connects the folder to your GitHub repository.

1. Download the project's **source ZIP**, then right-click it in Windows and select **Extract All**.
2. Open the extracted folder that contains `pom.xml`, `README.md`, `src`, and `resource-pack`.
3. Open GitHub Desktop, select **LegendarySwords**, and click **Show in Explorer**.
4. Copy the contents from step 2 into the folder opened in step 3. Copy the contents, rather than placing the whole extracted project as an extra nested folder. The existing repository should now contain `pom.xml` directly beside `.git`.
5. Return to GitHub Desktop. The source files should appear under **Changes**.
6. Enter `Add LegendarySwords plugin` in **Summary**, click **Commit to main**, then **Push origin** or **Publish branch**.

Do not commit the compiled JAR, the downloaded release ZIP, `target`, server worlds, or passwords. The source ZIP includes a `.gitignore` for generated build files.

## Build changes locally

Building the source requires a Java 21 **JDK**, rather than only a Java runtime, and Maven 3.9 or newer. After installing both, check them in a new terminal:

```sh
java -version
mvn -version
```

Both must use Java 21. In GitHub Desktop, select **Repository → Open in Terminal**, then run:

```sh
mvn --batch-mode verify
```

Upload the resulting `target/LegendarySwords-0.1.0.jar` to the server and restart it. When you change pack assets, also upload the rebuilt `target/LegendarySwords-resource-pack.zip` and update its SHA-1.
