/**
 * OpenSpec Manager Extension for Wakeve
 * Provides tools to list, view, create, and track OpenSpec proposals.
 */

import { readFileSync, readdirSync, writeFileSync, mkdirSync, existsSync } from "fs";
import { join } from "path";

const CHANGES_DIR = "openspec/changes";
const ARCHIVE_DIR = "openspec/archive";

interface OpenSpecEntry {
  id: string;
  status: "active" | "archived";
  path: string;
  hasProposal: boolean;
  hasTasks: boolean;
  hasDesign: boolean;
  specs: string[];
}

function listSpecs(dir: string, status: "active" | "archived"): OpenSpecEntry[] {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter(d => d.isDirectory())
    .map(d => {
      const p = join(dir, d.name);
      const files = readdirSync(p);
      const specsDir = join(p, "specs");
      return {
        id: d.name, status, path: p,
        hasProposal: files.includes("proposal.md"),
        hasTasks: files.includes("tasks.md"),
        hasDesign: files.includes("design.md"),
        specs: existsSync(specsDir) ? readdirSync(specsDir) : [],
      };
    });
}

export const name = "openspec-manager";
export const description = "Gestion des OpenSpec proposals Wakeve — liste, visualise, crée et suit les changements";

export const tools = [
  {
    name: "openspec_list",
    description: "Liste tous les OpenSpec proposals actifs et archivés avec leur statut",
    parameters: {
      type: "object",
      properties: {
        filter: {
          type: "string",
          enum: ["all", "active", "archived"],
          description: "Filtre par statut (défaut: all)",
        },
      },
    },
    execute: async ({ filter = "all" }: { filter?: string }) => {
      const active = filter !== "archived" ? listSpecs(CHANGES_DIR, "active") : [];
      const archived = filter !== "active" ? listSpecs(ARCHIVE_DIR, "archived") : [];
      const all = [...active, ...archived];

      if (all.length === 0) {
        return "Aucun OpenSpec trouvé. Utilise `openspec_create` pour en créer un.";
      }

      const lines: string[] = [];

      if (active.length > 0) {
        lines.push("## 🔄 Actifs\n");
        for (const spec of active) {
          lines.push(`### \`${spec.id}\``);
          lines.push([
            spec.hasProposal ? "📋 proposal" : "⬜ proposal",
            spec.hasTasks ? "✅ tasks" : "⬜ tasks",
            spec.hasDesign ? "🎨 design" : "",
            spec.specs.length > 0 ? `📁 specs(${spec.specs.join(",")})` : "",
          ].filter(Boolean).join(" · "));
          lines.push("");
        }
      }

      if (archived.length > 0) {
        lines.push("## 📦 Archivés\n");
        for (const spec of archived) {
          lines.push(`- \`${spec.id}\``);
        }
      }

      return lines.join("\n");
    },
  },

  {
    name: "openspec_view",
    description: "Affiche le contenu d'un OpenSpec (proposal, tasks, design, ou all)",
    parameters: {
      type: "object",
      properties: {
        changeId: { type: "string", description: "Identifiant du change (ex: add-transport-optimization)" },
        file: {
          type: "string",
          enum: ["proposal", "tasks", "design", "all"],
          description: "Fichier à afficher (défaut: proposal)",
        },
      },
      required: ["changeId"],
    },
    execute: async ({ changeId, file = "proposal" }: { changeId: string; file?: string }) => {
      let basePath = join(CHANGES_DIR, changeId);
      if (!existsSync(basePath)) {
        basePath = join(ARCHIVE_DIR, changeId);
        if (!existsSync(basePath)) {
          return `❌ OpenSpec \`${changeId}\` introuvable dans changes/ ni archive/.`;
        }
      }

      const filesToRead = file === "all" ? ["proposal.md", "tasks.md", "design.md"] : [`${file}.md`];
      const results: string[] = [];

      for (const filename of filesToRead) {
        const filePath = join(basePath, filename);
        if (existsSync(filePath)) {
          results.push(`## 📄 ${filename}\n\n${readFileSync(filePath, "utf-8")}`);
        }
      }

      const specsDir = join(basePath, "specs");
      if (existsSync(specsDir) && file === "all") {
        const specFiles = readdirSync(specsDir);
        if (specFiles.length > 0) {
          results.push(`## 📁 specs/\n${specFiles.map(f => `- ${f}`).join("\n")}`);
        }
      }

      return results.length > 0 ? results.join("\n\n---\n\n") : `❌ Aucun fichier \`${file}.md\` trouvé.`;
    },
  },

  {
    name: "openspec_create",
    description: "Crée un nouvel OpenSpec avec structure de répertoires et template proposal.md",
    parameters: {
      type: "object",
      properties: {
        changeId: { type: "string", description: "Identifiant kebab-case verb-led (ex: add-poll-reminders)" },
        title: { type: "string", description: "Titre human-readable" },
        platforms: {
          type: "array",
          items: { type: "string", enum: ["android", "ios", "server", "shared", "web"] },
          description: "Plateformes affectées",
        },
        effort: {
          type: "string",
          enum: ["S", "M", "L", "XL"],
          description: "Effort estimé",
        },
      },
      required: ["changeId", "title"],
    },
    execute: async ({
      changeId, title, platforms = [], effort = "M",
    }: { changeId: string; title: string; platforms?: string[]; effort?: string }) => {
      if (!/^[a-z][a-z0-9-]*$/.test(changeId)) {
        return `❌ Change ID invalide: \`${changeId}\`. Format: kebab-case verb-led`;
      }

      const changePath = join(CHANGES_DIR, changeId);
      if (existsSync(changePath)) {
        return `⚠️ OpenSpec \`${changeId}\` existe déjà.`;
      }

      mkdirSync(join(changePath, "specs"), { recursive: true });

      const allPlatforms = ["android", "ios", "server", "shared", "web"];
      const platformChecklist = [
        "Android (composeApp/)", "iOS (iosApp/)", "Server (server/)", "Shared (shared/)", "Web (webApp/)"
      ].map((p, i) => `- [${platforms.includes(allPlatforms[i]) ? "x" : " "}] ${p}`).join("\n");

      const proposal = `# ${changeId}: ${title}

## Problem Statement
TODO: Décris le problème que ce changement résout.

## Proposed Solution
TODO: Approche haut niveau de la solution.

## Technical Design

### Shared (KMP)
TODO

### Android
TODO

### iOS
TODO

### Server
TODO

### Web
TODO

## Database Changes
Aucun changement de schéma.

## Breaking Changes
Aucun.

## Success Criteria
- [ ] TODO: Critère mesurable 1

## Estimated Effort
${effort}

## Affected Platforms
${platformChecklist}
`;

      writeFileSync(join(changePath, "proposal.md"), proposal, "utf-8");

      return `✅ OpenSpec créé !

**Path:** \`${changePath}/\`

**Structure:**
\`\`\`
${changePath}/
├── proposal.md  ← 📝 À compléter
└── specs/
\`\`\`

Complète \`${changePath}/proposal.md\` puis délègue aux agents spécialistes.`;
    },
  },

  {
    name: "openspec_status",
    description: "Résumé du statut et avancement de tous les OpenSpecs actifs",
    parameters: { type: "object", properties: {} },
    execute: async () => {
      const active = listSpecs(CHANGES_DIR, "active");
      if (active.length === 0) return "✅ Aucun OpenSpec actif — le projet est clean !";

      const lines = ["# 📊 OpenSpec Status\n"];
      for (const spec of active) {
        const done = [spec.hasProposal, spec.hasTasks, spec.specs.length > 0].filter(Boolean).length;
        const bar = "█".repeat(done) + "░".repeat(3 - done);
        lines.push(`### \`${spec.id}\` [${bar}] ${done}/3`);
        lines.push([
          spec.hasProposal ? "✅ proposal" : "❌ proposal",
          spec.hasTasks ? "✅ tasks" : "⬜ tasks",
          spec.specs.length > 0 ? `✅ specs(${spec.specs.join(",")})` : "⬜ specs",
        ].join(" · "));
        lines.push("");
      }
      return lines.join("\n");
    },
  },
];
