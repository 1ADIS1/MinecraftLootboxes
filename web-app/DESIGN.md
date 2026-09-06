# Blockvault website

Open `/` on the running Spring Boot app (normally `http://localhost:8080/`). The catalogue now comes from the server, so opening the HTML file directly no longer loads cases. Routes: `#cases`, `#case/armour`, `#case/weapon`, `#case/tool`, `#login`, `#signup`.

The Java enums define the Minecraft Java 1.21.1 catalogue:

- `ArmorCase`: 30 items. All four slots in leather, chainmail, iron, gold, diamond, and netherite; turtle helmet; all four horse armour types; wolf armour.
- `WeaponCase`: 16 items. All six sword and axe tiers, bow, crossbow, trident, and mace.
- `ToolCase`: 31 items. All six pickaxe, axe, shovel, and hoe tiers; shears, flint and steel, fishing rod, brush, both steering sticks, and elytra.

This collection uses plain, unenchanted items. Newer-version equipment is intentionally excluded. References: [Minecraft Wiki armour](https://minecraft.wiki/w/Template:Navbox_armor), [tools](https://minecraft.wiki/w/Tool), [combat](https://minecraft.wiki/w/Tutorial:Combat), and [Paper 1.21.1 Material names](https://jd.papermc.io/paper/1.21.1/org/bukkit/Material.html).

`GET /api/cases` publishes the authoritative catalogue and drop chances. `CaseCatalog` allocates 10,000 integer probability units per case: common 45%, uncommon 35%, epic 15%, legendary 5%. Within a rarity, weights differ by at most 0.01 percentage points so the displayed item probabilities sum to exactly 100%. `CaseService` samples those same weights using `SecureRandom`.

`POST /open-case` accepts `caseId=armour|weapon|tool`. Identity comes from the logged-in session, and neither a client-selected reward nor a password is forwarded to Paper. The web app sends the selected material and amount 1 to the loopback plugin endpoint. Paper runs the player lookup and inventory mutation on its main thread, checks the player is online and has an empty storage slot, and responds with `200 Delivered` only after adding the item. Offline, full-inventory, and delivery failures are surfaced to the user. The app rejects the old plugin's premature `202` response.

After delivery confirmation, the reel animates right-to-left for 3,000 ms using a decelerating curve. Its central marker ends on the server-selected reward, and the result dialog opens only after the animation finishes. Reduced-motion mode preserves the three-second delay without scrolling motion. Decorative reel items do not determine the outcome. Concurrent clicks and navigation during an opening are blocked in the frontend; overlapping backend requests for the same user are rejected.

Delivery happens before the visual reveal. If confirmation is lost, check the player's inventory; the app never automatically retries an uncertain delivery. This is not a persistent transaction ledger and there are no payments or keys in this implementation.

## Run and verify

Rebuild/restart both the web app and Paper plugin after this change. Gradle's `runServer` task loads the newly built plugin; for a manually managed server, copy the newly built root `build/libs` plugin JAR into that server's `plugins` directory before restarting. Keep both processes on the same machine for the default loopback endpoint. The web endpoint can be configured with `lootboxes.plugin-url`.

Log in with the online player's exact Minecraft username, leave an inventory slot empty, choose a case, and press **Open!**.

Focused checks from the repository root:

```text
gradlew.bat classes jar :web-app:test --tests com.application.CaseCatalogTest --tests com.application.CaseServiceTest --tests com.application.CaseControllerTest
node --test web-app/src/test/js/case-opening.test.cjs
```

The HTTP delivery tests use a local stub server; an actual logged-in Minecraft client is still needed for an end-to-end gameplay check.

Account forms submit URL-encoded requests to the existing `/login` and `/add` endpoints, using the existing session cookie. No password is stored by the frontend. Backend authentication was not changed by this design work.

## Generated artwork

Created with the built-in image-generation tool. Final assets: `src/main/resources/static/images/cases/armour.png`, `weapon.png`, `tool.png`. Existing item sprites are reused from `static/images/item/` with pixelated rendering.

Shared prompt:

> Use case: stylized-concept. Create one square Minecraft-style voxel game case illustration for a dark premium case-opening website. Isometric three-quarter view of an open chunky treasure chest, finely pixel-textured blocks, dramatic cinematic lighting, floating tiny square particles, crisp cubic geometry, no text, no UI, no watermark. Center whole object with generous margins on solid near-black charcoal #111612 background.

Per-image prompt suffixes:

- Armour: Armour case: iron-banded dark wooden chest with cyan diamond chestplate and helmet emerging, cyan mint light from inside, sturdy defensive identity.
- Weapon: Weapon case: blackstone chest with purple metal bands, crossed diamond sword and axe emerging, rich violet magical light.
- Tool: Tool case: oak chest with copper bands, diamond pickaxe and shovel emerging, golden amber light, adventurous mining identity.
