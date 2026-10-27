// ==========================================================================
// TAKASHA RPG — APPLICATION LOGIC & INTERACTIVE SIMULATORS
// ==========================================================================

document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  initEnchantsExplorer();
  initLevelledMobsSimulator();
  initTitlesAndQuests();
  initEconomyAndSlots();
});

// --------------------------------------------------------------------------
// 1. Navigation Tabs
// --------------------------------------------------------------------------
function initTabs() {
  const navBtns = document.querySelectorAll(".nav-btn");
  const tabPanels = document.querySelectorAll(".tab-panel");

  navBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-tab");

      navBtns.forEach(b => b.classList.remove("active"));
      tabPanels.forEach(p => p.classList.remove("active"));

      btn.classList.add("active");
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add("active");
      }
    });
  });

  // Sub-tabs in Tab 3 (Titles & Quests)
  const subNavBtns = document.querySelectorAll(".sub-nav-btn");
  const subPanels = document.querySelectorAll(".sub-panel");

  subNavBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-sub");

      subNavBtns.forEach(b => b.classList.remove("active"));
      subPanels.forEach(p => p.classList.remove("active"));

      btn.classList.add("active");
      const targetPanel = document.getElementById(targetId);
      if (targetPanel) {
        targetPanel.classList.add("active");
      }
    });
  });
}

// --------------------------------------------------------------------------
// 2. 334 Custom Enchants Explorer
// --------------------------------------------------------------------------
function initEnchantsExplorer() {
  const grid = document.getElementById("enchants-grid");
  const searchInput = document.getElementById("enchant-search");
  const tierFilters = document.querySelectorAll("#tier-filters .filter-tag");
  const appliesFilters = document.querySelectorAll("#applies-filters .filter-tag");
  const visibleCountEl = document.getElementById("visible-count");

  let currentTier = "ALL";
  let currentApplies = "ALL";
  let searchQuery = "";

  const tierGlowMap = {
    Simple: "rgba(156, 163, 175, 0.25)",
    Unique: "rgba(74, 222, 128, 0.35)",
    Elite: "rgba(56, 189, 248, 0.35)",
    Ultimate: "rgba(192, 132, 252, 0.4)",
    Legendary: "rgba(251, 146, 60, 0.45)",
    Fabled: "rgba(232, 121, 249, 0.45)",
    Heroic: "rgba(244, 63, 94, 0.5)",
    Soul: "rgba(6, 182, 212, 0.5)",
    Mastery: "rgba(255, 215, 0, 0.6)"
  };

  const tierColorMap = {
    Simple: "#9ca3af",
    Unique: "#4ade80",
    Elite: "#38bdf8",
    Ultimate: "#c084fc",
    Legendary: "#fb923c",
    Fabled: "#e879f9",
    Heroic: "#f43f5e",
    Soul: "#06b6d4",
    Mastery: "#ffd700"
  };

  function renderEnchants() {
    grid.innerHTML = "";

    const filtered = TAKASHA_ENCHANTS.filter(e => {
      // Tier match
      if (currentTier !== "ALL" && e.tier.toLowerCase() !== currentTier.toLowerCase()) {
        return false;
      }
      // Applies match
      if (currentApplies !== "ALL") {
        if (!e.applies.toLowerCase().includes(currentApplies.toLowerCase())) {
          return false;
        }
      }
      // Search match
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const inName = e.name.toLowerCase().includes(q);
        const inDesc = e.description.toLowerCase().includes(q);
        const inApp = e.applies.toLowerCase().includes(q);
        if (!inName && !inDesc && !inApp) {
          return false;
        }
      }
      return true;
    });

    visibleCountEl.textContent = filtered.length;

    if (filtered.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-dim);">
          <div style="font-size: 32px; margin-bottom: 10px;">🔍</div>
          <div>Tidak ada enchantment yang cocok dengan kriteria pencarian.</div>
        </div>
      `;
      return;
    }

    // Render cards efficiently
    const fragment = document.createDocumentFragment();

    filtered.forEach(ench => {
      const card = document.createElement("div");
      card.className = "enchant-card";
      const color = tierColorMap[ench.tier] || "#cbd5e1";
      const glow = tierGlowMap[ench.tier] || "rgba(255,255,255,0.1)";

      card.style.setProperty("--card-tier-color", color);
      card.style.setProperty("--card-tier-glow", glow);

      card.innerHTML = `
        <div class="card-top">
          <span class="enchant-name">${escapeHtml(ench.name)}</span>
          <span class="enchant-max">Max Lvl ${ench.maxLevel}</span>
        </div>
        <div class="enchant-desc">${escapeHtml(ench.description)}</div>
        <div class="card-bottom">
          <span class="tier-badge tier-${ench.tier}">${ench.tier}</span>
          <span class="applies-badge">🎯 ${escapeHtml(ench.applies)}</span>
        </div>
      `;
      fragment.appendChild(card);
    });

    grid.appendChild(fragment);
  }

  // Event Listeners
  searchInput.addEventListener("input", (e) => {
    searchQuery = e.target.value.trim();
    renderEnchants();
  });

  tierFilters.forEach(tag => {
    tag.addEventListener("click", () => {
      tierFilters.forEach(t => t.classList.remove("active"));
      tag.classList.add("active");
      currentTier = tag.getAttribute("data-tier");
      renderEnchants();
    });
  });

  appliesFilters.forEach(tag => {
    tag.addEventListener("click", () => {
      appliesFilters.forEach(t => t.classList.remove("active"));
      tag.classList.add("active");
      currentApplies = tag.getAttribute("data-applies");
      renderEnchants();
    });
  });

  // Initial render
  renderEnchants();
}

// --------------------------------------------------------------------------
// 3. LevelledMobs Lvl 1–1000 Simulator
// --------------------------------------------------------------------------
function initLevelledMobsSimulator() {
  const sliderSkill = document.getElementById("slider-skill");
  const sliderDistance = document.getElementById("slider-distance");
  const selectDimension = document.getElementById("select-dimension");

  const valSkill = document.getElementById("val-skill");
  const valDistance = document.getElementById("val-distance");

  const resMobLevel = document.getElementById("res-mob-level");
  const resMobTier = document.getElementById("res-mob-tier");
  const resMobHp = document.getElementById("res-mob-hp");
  const resMobHpHearts = document.getElementById("res-mob-hp-hearts");
  const resMobAtk = document.getElementById("res-mob-atk");
  const resMobArmorPierce = document.getElementById("res-mob-armor-pierce");
  const resMobCap = document.getElementById("res-mob-cap");
  const nametagPreview = document.getElementById("nametag-preview");
  const dropList = document.getElementById("res-drop-list");

  function calculate() {
    const sk = parseInt(sliderSkill.value, 10);
    const dist = parseInt(sliderDistance.value, 10);
    const dimMult = parseFloat(selectDimension.value);

    valSkill.textContent = `${sk} SK Avg`;
    valDistance.textContent = `${dist.toLocaleString()} Blok`;

    // 1. Calculate Mob Level (1 - 1000)
    // Formula: (sk * 0.55) + (dist / 35) * dimMult
    let rawLevel = Math.round((sk * 0.55) + (dist / 35) * dimMult);
    const mobLevel = Math.max(1, Math.min(1000, rawLevel));

    // 2. Non-linear HP calculation
    let calculatedHp = 20;
    if (mobLevel <= 250) {
      calculatedHp = 20 * (1 + mobLevel * 0.015);
    } else if (mobLevel <= 500) {
      const hp250 = 20 * (1 + 250 * 0.015);
      calculatedHp = hp250 * (1 + (mobLevel - 250) * 0.022);
    } else if (mobLevel <= 750) {
      const hp250 = 20 * (1 + 250 * 0.015);
      const hp500 = hp250 * (1 + 250 * 0.022);
      calculatedHp = hp500 * (1 + (mobLevel - 500) * 0.032);
    } else {
      const hp250 = 20 * (1 + 250 * 0.015);
      const hp500 = hp250 * (1 + 250 * 0.022);
      const hp750 = hp500 * (1 + 250 * 0.032);
      calculatedHp = hp750 * (1 + (mobLevel - 750) * 0.045);
    }
    const finalHp = Math.round(calculatedHp);
    const hearts = Math.round(finalHp / 2);

    // 3. Attack Damage & True Damage
    const baseAtk = (3.0 + (mobLevel * 0.35)).toFixed(1);
    let trueDmgPercent = 0;
    if (mobLevel >= 901) trueDmgPercent = 30;
    else if (mobLevel >= 751) trueDmgPercent = 20;
    else if (mobLevel >= 600) trueDmgPercent = 15;

    // 4. Anti-Melt Cap per Hit (15% Max HP on Bosses 750-1000)
    let antiMeltText = "None";
    if (mobLevel >= 750) {
      const capDmg = Math.round(finalHp * 0.15);
      antiMeltText = `Max ${capDmg} DMG / hit (15% HP)`;
    }

    // 5. Tier & Nametag Preview
    let tierName = "Common Novice";
    let tierColor = "#9ca3af";
    let mobTitle = "Zombie";
    let nametagHtml = "";

    if (mobLevel <= 150) {
      tierName = "Common Wanderer";
      tierColor = "#9ca3af";
      mobTitle = "Undead Raider";
      nametagHtml = `<span style="color:#9ca3af">[Lvl ${mobLevel}]</span> <span style="color:#e2e8f0">${mobTitle}</span> <span style="color:#4ade80">[${finalHp}/${finalHp} HP]</span>`;
    } else if (mobLevel <= 350) {
      tierName = "Uncommon Corrupted";
      tierColor = "#4ade80";
      mobTitle = "Corrupted Berserker";
      nametagHtml = `<span style="color:#4ade80">[Lvl ${mobLevel}]</span> <span style="color:#22c55e">⚔️ ${mobTitle}</span> <span style="color:#a3e635">[${finalHp}/${finalHp} HP]</span>`;
    } else if (mobLevel <= 550) {
      tierName = "Elite Ancient";
      tierColor = "#38bdf8";
      mobTitle = "Ancient Relic Guardian";
      nametagHtml = `<span style="color:#38bdf8">[Lvl ${mobLevel}]</span> <span style="color:#0284c7">⚡ ${mobTitle}</span> <span style="color:#38bdf8">[${finalHp}/${finalHp} HP]</span>`;
    } else if (mobLevel <= 750) {
      tierName = "Epic Void Fiend";
      tierColor = "#c084fc";
      mobTitle = "Void Scourge Behemoth";
      nametagHtml = `<span style="color:#c084fc">[Lvl ${mobLevel}]</span> <span style="color:#9333ea">🔮 ${mobTitle}</span> <span style="color:#c084fc">[${finalHp}/${finalHp} HP]</span>`;
    } else if (mobLevel <= 900) {
      tierName = "Legendary Infernal Titan";
      tierColor = "#fb923c";
      mobTitle = "Infernal Titan Sovereign";
      nametagHtml = `<span style="color:#fb923c">[Lvl ${mobLevel}]</span> <span style="color:#ea580c">🔥 ${mobTitle}</span> <span style="color:#fb923c">[${finalHp}/${finalHp} HP]</span>`;
    } else {
      tierName = "Mythic Cataclysm Overlord (World Boss)";
      tierColor = "#f43f5e";
      mobTitle = "⚔️ Cataclysm Sovereign Overlord ⚔️";
      nametagHtml = `<span style="color:#f43f5e; font-weight:700;">[Lvl ${mobLevel}]</span> <span style="color:#e11d48; font-weight:700;">💀 ${mobTitle} 💀</span> <span style="color:#fb7185">[${finalHp.toLocaleString()}/${finalHp.toLocaleString()} HP]</span>`;
    }

    // Update UI elements
    resMobLevel.textContent = `Lvl ${mobLevel}`;
    resMobLevel.style.color = tierColor;
    resMobTier.textContent = `Tier: ${tierName}`;
    resMobHp.textContent = `${finalHp.toLocaleString()} HP`;
    resMobHpHearts.textContent = `${hearts.toLocaleString()} Hearts (Darah)`;
    resMobAtk.textContent = `${baseAtk} DMG`;
    resMobArmorPierce.textContent = trueDmgPercent > 0 ? `+${trueDmgPercent}% True DMG (Tembus Armor)` : `0% True DMG (Fisik)`;
    resMobArmorPierce.style.color = trueDmgPercent > 0 ? "#f43f5e" : "var(--text-muted)";
    resMobCap.textContent = antiMeltText;
    resMobCap.style.color = mobLevel >= 750 ? "#38bdf8" : "var(--text-muted)";

    nametagPreview.innerHTML = nametagHtml;

    // Drop matrix
    const coinDrop = mobLevel * 5;
    const expDrop = mobLevel * 8;
    let drops = [
      `<span>🪙 Takasha Gold Coins</span><span class="drop-rate">${coinDrop.toLocaleString()} Coins</span>`,
      `<span>✨ AuraSkills / Character EXP</span><span class="drop-rate">${expDrop.toLocaleString()} EXP</span>`
    ];

    if (mobLevel >= 200) {
      drops.push(`<span>⚗️ Magic Dust (Peluang ${Math.min(85, Math.round(mobLevel / 10))}%)</span><span class="drop-rate">Tier ${tierName.split(" ")[0]}</span>`);
    }
    if (mobLevel >= 450) {
      drops.push(`<span>💎 Arcane Gem (Peluang ${Math.min(50, Math.round(mobLevel / 20))}%)</span><span class="drop-rate">1–3 Gems</span>`);
    }
    if (mobLevel >= 700) {
      drops.push(`<span>📜 White Scroll / Soul Fragment (Peluang 25%)</span><span class="drop-rate">Rare Relic</span>`);
    }
    if (mobLevel >= 900) {
      drops.push(`<span>👑 Master Slot Increaser / Relic Core (Peluang 15%)</span><span class="drop-rate">Endgame Loot</span>`);
    }

    dropList.innerHTML = drops.map(d => `<li>${d}</li>`).join("");
  }

  sliderSkill.addEventListener("input", calculate);
  sliderDistance.addEventListener("input", calculate);
  selectDimension.addEventListener("change", calculate);

  calculate();
}

// --------------------------------------------------------------------------
// 4. 16 Titles & Quests
// --------------------------------------------------------------------------
function initTitlesAndQuests() {
  const titlesGrid = document.getElementById("titles-grid");
  const legendaryGrid = document.getElementById("legendary-quests-grid");
  const dailyList = document.getElementById("daily-quests-list");
  const weeklyList = document.getElementById("weekly-quests-list");

  // Render 16 Titles
  if (titlesGrid && typeof RPG_TITLES !== "undefined") {
    titlesGrid.innerHTML = RPG_TITLES.map(title => `
      <div class="title-card" style="--title-glow-color: ${title.color}40;">
        <div>
          <div class="title-card-top">
            <div class="title-icon">${title.icon}</div>
            <div class="title-header">
              <h3>${title.name}</h3>
              <div class="title-roman">${title.code} • ${title.roman}</div>
            </div>
          </div>
          <div class="title-trait-box">
            <span class="trait-badge" style="color: ${title.color};">⚡ ${title.traitName}</span>
            <div class="trait-desc">${title.traitDesc}</div>
          </div>
        </div>
        <div class="title-footer">
          <span class="title-rarity" style="color: ${title.color};">${title.rarity}</span>
          <span class="title-cd">⏱️ ${title.cooldown}</span>
        </div>
      </div>
    `).join("");
  }

  // Render 16 Legendary Quests
  if (legendaryGrid && typeof RPG_QUESTS !== "undefined") {
    legendaryGrid.innerHTML = RPG_QUESTS.legendary.map(q => `
      <div class="quest-card">
        <div class="quest-top">
          <div class="quest-icon">${q.icon}</div>
          <div class="quest-title-wrap">
            <span class="quest-code">${q.code}</span>
            <h4>${q.title}</h4>
          </div>
        </div>
        <div class="quest-desc">${q.desc}</div>
        <div class="quest-reward">
          <span>🎁</span>
          <span>${q.reward}</span>
        </div>
      </div>
    `).join("");
  }

  // Render Daily & Weekly Quests
  if (dailyList && weeklyList && typeof RPG_QUESTS !== "undefined") {
    dailyList.innerHTML = RPG_QUESTS.daily.map(q => `
      <div class="routine-card">
        <div class="routine-head">${q.icon} ${q.title}</div>
        <div class="routine-desc">${q.desc}</div>
        <div class="routine-rew">🎁 ${q.reward}</div>
      </div>
    `).join("");

    weeklyList.innerHTML = RPG_QUESTS.weekly.map(q => `
      <div class="routine-card">
        <div class="routine-head">${q.icon} ${q.title}</div>
        <div class="routine-desc">${q.desc}</div>
        <div class="routine-rew">🎁 ${q.reward}</div>
      </div>
    `).join("");
  }
}

// --------------------------------------------------------------------------
// 5. ShopGUI & Slots System
// --------------------------------------------------------------------------
function initEconomyAndSlots() {
  const slotsBar = document.getElementById("slots-bar");
  const btnMinus = document.getElementById("btn-slot-minus");
  const btnPlus = document.getElementById("btn-slot-plus");
  const simSlotCount = document.getElementById("sim-slot-count");
  const simSlotDesc = document.getElementById("sim-slot-desc");

  const convCoins = document.getElementById("conv-coins");
  const convGems = document.getElementById("conv-gems");
  const shopTableBody = document.getElementById("shop-table-body");

  let currentSlotCount = 10;

  function renderSlots() {
    slotsBar.innerHTML = "";
    for (let i = 1; i <= 20; i++) {
      const slotEl = document.createElement("div");
      slotEl.className = "slot-item";

      if (i <= currentSlotCount) {
        slotEl.classList.add("unlocked");
        slotEl.innerHTML = `<span>#${i}</span><span>🔓</span>`;
      } else {
        slotEl.classList.add("locked");
        slotEl.innerHTML = `<span>#${i}</span><span>🔒</span>`;
      }
      slotsBar.appendChild(slotEl);
    }

    simSlotCount.textContent = `${currentSlotCount} Slots`;

    if (currentSlotCount <= 10) {
      simSlotDesc.textContent = `Status: ${currentSlotCount} Slot Bawaan Polos Aktif tanpa biaya tambahan.`;
      simSlotDesc.style.color = "var(--text-dim)";
    } else if (currentSlotCount <= 15) {
      const added = currentSlotCount - 10;
      simSlotDesc.textContent = `Status: 10 Bawaan + ${added} Regular Increaser terpasang (Batas Tier 1).`;
      simSlotDesc.style.color = "#38bdf8";
    } else {
      const masterAdded = currentSlotCount - 15;
      simSlotDesc.textContent = `Status: 10 Bawaan + 5 Regular + ${masterAdded} Master Increaser terpasang (Hard Cap Puncak: 20 Slots).`;
      simSlotDesc.style.color = "var(--gold-accent)";
    }
  }

  btnMinus.addEventListener("click", () => {
    if (currentSlotCount > 10) {
      currentSlotCount--;
      renderSlots();
    }
  });

  btnPlus.addEventListener("click", () => {
    if (currentSlotCount < 20) {
      currentSlotCount++;
      renderSlots();
    }
  });

  renderSlots();

  // Currency Converter (500 Coins = 1 Arcane Gem)
  convCoins.addEventListener("input", (e) => {
    const val = parseFloat(e.target.value) || 0;
    convGems.value = Math.floor(val / 500);
  });

  convGems.addEventListener("input", (e) => {
    const val = parseFloat(e.target.value) || 0;
    convCoins.value = val * 500;
  });

  // Render ShopGUI items
  if (shopTableBody && typeof RPG_SHOP !== "undefined") {
    shopTableBody.innerHTML = RPG_SHOP.map(item => `
      <tr>
        <td><strong>${item.name}</strong></td>
        <td><span class="applies-badge">${item.category}</span></td>
        <td style="color: var(--text-muted);">${item.desc}</td>
        <td class="shop-price">${item.price}</td>
      </tr>
    `).join("");
  }
}

// Helper: Escape HTML string to prevent injection
function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
}
