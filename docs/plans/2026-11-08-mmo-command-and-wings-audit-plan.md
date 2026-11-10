# Plan: Fix MMO Commands & Audit/Implement Wings Flight Enchantments

## Goal
Fix `/mmo` command accessibility for all players, implement the `wings` flight enchantment engine alongside audited movement enchantments, and deliver a clean `PANDUAN_TEST_TAKASHA_RPG.txt` with updated PRD, VERSION, and CHANGELOG.

## Tasks
- [x] Task 1: Fix `MMOCommand.java` permissions & arguments → Verify: `/mmo`, `/mmo list`, `/mmo give <item_id>`, `/mmo give <player> <item_id>`, `/mmo reload`, and `/rpg mmo` accessible without OP in Survival.
- [x] Task 2: Implement `wings` & `takeoff` flight mechanics in `TakashaEnchantUtilityEngine.java` → Verify: Equipping boots with `wings` grants `mayfly=true`; removing boots revokes flight and prevents fall damage.
- [x] Task 3: Implement audited movement & escape enchantments (`antigravity`, `rocketescape`, `guidedrocketescape`, `launch`, `ascend`, `slingshot`) in `TakashaEnchantUtilityEngine.java` & `TakashaEnchantCombatEngine.java` → Verify: Jump boost, rocket launch on low HP, and right-click mobility trigger properly.
- [x] Task 4: Rebuild and replace `PANDUAN_TEST_TAKASHA_RPG.txt` → Verify: File cleared and populated with concise, step-by-step test procedures for MMO items, ballistics, and Wings flight enchantments.
- [x] Task 5: Synchronize project metadata in `VERSION`, `takasha-rpg/build.gradle`, `PRD.md`, and `CHANGELOG.md` → Verify: Version bumped to 2.8.2, changelog details REA audit & fixes, PRD updated.
- [x] Task 6: Compile & verify mod build (`.\gradlew.bat :takasha-rpg:build`) → Verify: Build succeeds 100%, generates updated `Hasil mod/Takasha-RPG-1.8.1+26.2.jar`.

## Done When
- [x] `/mmo` commands work seamlessly in Survival and Creative modes with full tab-completion.
- [x] Equipping boots with `wings` allows players to fly like in Creative mode and revokes flight safely upon removal.
- [x] `PANDUAN_TEST_TAKASHA_RPG.txt` contains exclusively current testing tasks.
- [x] Documentation (`PRD.md`, `CHANGELOG.md`, `VERSION`) is synchronized.
- [x] Mod jar compiles with 0 errors and is copied to `Hasil mod/`.

## Notes
- Flight capability requires syncing `ServerPlayer.onUpdateAbilities()` and managing edge cases (e.g. creative mode override, disconnection, fall damage cancellation).
- REA analysis revealed root `.requires` in Brigadier was causing `/mmo` to be hidden from non-OP clients.
