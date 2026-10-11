# Boss health, hitboxes and inventory editing update

LegendarySwords 0.5.4 adds temporary max-health bonuses during boss ultimates: Angel +80 HP, Colossus +120 HP, Leviathan/Dragon/Frost Titan +100 HP. Two HP equal one heart. Existing base health and other modifiers are preserved; the bonus is removed when the form ends. Configure `bosses.health-bonus` and `bosses.health-bonus-by-form.<weapon-id>`.

Invisible damage receivers cover each boss body, wings/fins and tail, forwarding attacks to its player with normal armor and protection handling. Coverage approximates the resting shape with boxes; it does not follow every animated bone or enlarge movement collision. Configure `bosses.hitboxes.enabled`, `update-interval-ticks` (default 2), and optional `parts.<weapon-id>` lists of `right`, `up`, `back`, `width` (block units). These features activate during LegendarySwords boss ultimates, not manual `/bm disguise`.

LegendaryAdmin 0.1.2 adds `/ecsee <online-player>` and live editing for both `/invsee` and `/ecsee`. Left/right click to take or put items. Shift transfers, drags and hotbar swaps into the target view are blocked. Permissions: `legendaryadmin.invsee`, `legendaryadmin.ecsee`, or `legendaryadmin.admin` (operators by default).

Stop the server. Replace the old LegendarySwords and LegendaryAdmin JARs with the two included JARs; keep their data folders. Restart. Existing models and resource packs remain compatible and do not need replacement.

Validation: 335 LegendarySwords tests and 16 LegendaryAdmin tests passed. No live Minecraft client verification.
