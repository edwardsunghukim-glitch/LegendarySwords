LegendarySwords 0.2.4: character animations only during casts

BetterModel 3.1 requires Java 25 on the Paper 1.21.11 server.
abilities-only and cast-body-animations default to true, including old configs.
Walking, running, standing, ordinary swings and manual poses use normal movement.
Only ability/ultimate character clips create a temporary body. It restores at
clip/scene end. An ultimate holds its final pose until the shared scene ends.
Legacy player-body-animations is used only when abilities-only is false.

Scenes charge and reveal the blade ahead of the caster, then release ultimate
attack/damage once. Nearby visible players receive the shared scene title and
see world effects. /ls cinematics on enables your scene without toggling it off;
/ls cinematics status reports your preference. Server cinematics.enabled must
also be true. Positions/game modes/cameras are unchanged. Use F5 for body clips.

The inventory-copy/server-thread snapshot hardening remains. Cast bodies still
use the equipment/disguise path and require live-client testing for the earlier
disconnect. Set cast-body-animations false if it recurs; scenes and attack effects
remain without player body trackers. First-person arm swings remain vanilla.

All sixteen .bbmodel files and resource-pack assets are unchanged from 0.2.2.
Reuse the existing 0.2.2 combined ZIP. No new build.zip is required unless other
models or generated-client settings changed. The JAR installs known original
models with backups and preserves edits. For manual updates, copy players files
to BetterModel/players and slash models to BetterModel/models, then /bm reload.
