# TotemLimit 1.0.0

Paper 1.21.11, Java 21+. Limits the total carried amount to **2 totems** and **32 ender pearls** by default. Inventory, offhand, armor slots and cursor count; stored chest/ender chest items do not. Excess is dropped at the player with a short pickup delay, preserving metadata. Full-limit pickups are blocked. Items granted by commands, kits or inventory editing are checked on the next server tick.

Configure `plugins/TotemLimit/config.yml`: `limits.totems`, `limits.ender-pearls` (zero disallows carrying the item), `check-interval-ticks` (1–20, requires restart). Use `/totemlimit reload` for amount changes. `totemlimit.admin` defaults to operators; `totemlimit.bypass` defaults to nobody, including operators.

Install TotemLimit-1.0.0.jar in plugins and restart. No resource pack or dependencies. Build and tests: `mvn verify`.
