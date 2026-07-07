/**
 * Wakeve Health Dashboard Extension
 * Project health: accessibility audit, OpenSpec tracking, tasks, git status.
 */

import { readFileSync, existsSync, readdirSync } from "fs";
import { execSync } from "child_process";

// Exact audit_report.json schema
interface AuditIssue {
  id: string;
  category: string;
  priority: "critical" | "major";
  description: string;
  line: number;
  wcag_criterion: string;
  fix_time_minutes: number;
  contrast_ratio?: number | string;
  required_ratio?: number;
}

interface AuditScreen {
  name: string;
  file: string;
  status: "NEEDS_FIXES" | "APPROVED";
  critical: number;
  major: number;
  issues: AuditIssue[];
}

interface AuditSummary {
  overall_status: string;
  screens_approved: number;
  screens_failing: number;
  critical_issues: number;
  major_issues: number;
  estimated_fix_time_minutes: number;
}

interface AuditReport {
  summary: AuditSummary;
  screens: AuditScreen[];
}

function safeExec(cmd: string): string {
  try { return execSync(cmd, { encoding: "utf-8", timeout: 8000 }).trim(); }
  catch { return ""; }
}

function readJson<T>(path: string): T | null {
  if (!existsSync(path)) return null;
  try { return JSON.parse(readFileSync(path, "utf-8")) as T; }
  catch { return null; }
}

function countDirs(dir: string): number {
  if (!existsSync(dir)) return 0;
  try { return readdirSync(dir, { withFileTypes: true }).filter(d => d.isDirectory()).length; }
  catch { return 0; }
}

export const name = "wakeve-health";
export const description = "Dashboard santé du projet Wakeve — accessibilité, OpenSpec, git, tâches";

export const tools = [
  {
    name: "wakeve_health",
    description: "Tableau de bord complet de la santé du projet Wakeve",
    parameters: {
      type: "object",
      properties: {
        section: {
          type: "string",
          enum: ["all", "accessibility", "openspec", "tasks", "git"],
          description: "Section à afficher (défaut: all)",
        },
      },
    },
    execute: async ({ section = "all" }: { section?: string }) => {
      const lines: string[] = [
        "# 🏥 Wakeve Project Health",
        `*${new Date().toISOString().split("T")[0]}*\n`,
      ];

      // ACCESSIBILITY
      if (section === "all" || section === "accessibility") {
        lines.push("## ♿ Accessibilité (WCAG 2.1 AA)");
        const audit = readJson<AuditReport>("audit_report.json");
        if (audit?.summary) {
          const s = audit.summary;
          const emoji = s.overall_status === "APPROVED" ? "✅" : "❌";
          lines.push(`**Status:** ${emoji} ${s.overall_status}`);
          lines.push(`🔴 ${s.critical_issues} critiques · 🟡 ${s.major_issues} majeures · ⏱️ ~${s.estimated_fix_time_minutes} min`);
          lines.push(`✅ ${s.screens_approved} screens approuvés · ❌ ${s.screens_failing} à corriger`);

          if (audit.screens?.length) {
            lines.push("\n| Screen | Status | Critical | Major |");
            lines.push("|--------|--------|----------|-------|");
            for (const sc of audit.screens) {
              const e = sc.status === "APPROVED" ? "✅" : "❌";
              lines.push(`| ${sc.name} | ${e} | ${sc.critical} | ${sc.major} |`);
            }
          }
        } else {
          lines.push("⚠️ `audit_report.json` non trouvé — lance un audit WCAG.");
        }
        lines.push("");
      }

      // OPENSPEC
      if (section === "all" || section === "openspec") {
        lines.push("## 📋 OpenSpec");
        const active = countDirs("openspec/changes");
        const archived = countDirs("openspec/archive");
        lines.push(`🔄 ${active} actifs · 📦 ${archived} archivés`);
        if (active > 0 && existsSync("openspec/changes")) {
          const dirs = readdirSync("openspec/changes", { withFileTypes: true }).filter(d => d.isDirectory());
          for (const d of dirs) {
            const hasTasks = existsSync(`openspec/changes/${d.name}/tasks.md`);
            lines.push(`- ${hasTasks ? "🔨" : "📝"} \`${d.name}\``);
          }
        }
        lines.push("");
      }

      // GIT
      if (section === "all" || section === "git") {
        lines.push("## 🔀 Git");
        const branch = safeExec("git rev-parse --abbrev-ref HEAD");
        if (branch) {
          lines.push(`**Branch:** \`${branch}\``);
          const commit = safeExec('git log -1 --pretty=format:"%h %s"');
          if (commit) lines.push(`**Dernier commit:** ${commit}`);
          const dirty = safeExec("git status --porcelain | wc -l").trim();
          if (dirty !== "0") lines.push(`⚠️ ${dirty} fichier(s) non commités`);
          const ahead = safeExec("git rev-list @{u}..HEAD --count 2>/dev/null");
          if (ahead && ahead !== "0") lines.push(`📤 ${ahead} commit(s) à pusher`);
        } else {
          lines.push("⚠️ Pas de dépôt git");
        }
        lines.push("");
      }

      // TASKS
      if (section === "all" || section === "tasks") {
        lines.push("## 📝 Tâches");
        if (existsSync("tasks.md")) {
          const content = readFileSync("tasks.md", "utf-8");
          const lineCount = content.split("\n").length;
          const checked = (content.match(/- \[x\]/gi) || []).length;
          const unchecked = (content.match(/- \[ \]/g) || []).length;
          lines.push(`${lineCount} lignes · ✅ ${checked} faites · ⬜ ${unchecked} restantes`);
        } else {
          lines.push("⚠️ tasks.md non trouvé");
        }
        lines.push("");
      }

      // GLOBAL SCORE
      if (section === "all") {
        const audit = readJson<AuditReport>("audit_report.json");
        const criticalIssues = audit?.summary?.critical_issues ?? 99;

        lines.push("---\n## 🎯 Score Global");
        const checks: [string, boolean][] = [
          ["♿ Audit présent", existsSync("audit_report.json")],
          ["♿ 0 issues critiques", criticalIssues === 0],
          ["📋 Pas d'OpenSpec bloquants", countDirs("openspec/changes") === 0],
          ["📝 Tasks trackées", existsSync("tasks.md")],
          ["🔀 Git propre", safeExec("git status --porcelain").trim() === ""],
        ];

        const passed = checks.filter(([, ok]) => ok).length;
        const score = Math.round((passed / checks.length) * 100);
        const emoji = score >= 80 ? "🟢" : score >= 50 ? "🟡" : "🔴";
        lines.push(`**${emoji} ${score}% (${passed}/${checks.length})**\n`);
        for (const [label, ok] of checks) lines.push(`${ok ? "✅" : "❌"} ${label}`);
      }

      return lines.join("\n");
    },
  },

  {
    name: "wakeve_accessibility_detail",
    description: "Détails des violations WCAG pour un screen spécifique ou tous les screens qui échouent",
    parameters: {
      type: "object",
      properties: {
        screen: {
          type: "string",
          description: "Nom du screen (ex: ModernHomeView) ou 'failing' pour tous les screens NEEDS_FIXES",
        },
      },
      required: ["screen"],
    },
    execute: async ({ screen }: { screen: string }) => {
      const audit = readJson<AuditReport>("audit_report.json");
      if (!audit) return "❌ `audit_report.json` non trouvé.";

      const matchingScreens = screen === "failing"
        ? (audit.screens ?? []).filter(s => s.status === "NEEDS_FIXES")
        : (audit.screens ?? []).filter(s => s.name.toLowerCase().includes(screen.toLowerCase()));

      if (matchingScreens.length === 0) {
        return screen === "failing"
          ? "✅ Tous les screens sont approuvés !"
          : `❌ Screen \`${screen}\` non trouvé. Screens disponibles: ${(audit.screens ?? []).map(s => s.name).join(", ")}`;
      }

      const lines: string[] = [];
      for (const s of matchingScreens) {
        lines.push(`## ${s.status === "APPROVED" ? "✅" : "❌"} ${s.name}`);
        lines.push(`*Fichier: \`${s.file}\`*\n`);

        const critIssues = s.issues.filter(i => i.priority === "critical");
        const majIssues = s.issues.filter(i => i.priority === "major");

        if (critIssues.length > 0) {
          lines.push("### 🔴 Issues Critiques");
          for (const i of critIssues) {
            lines.push(`\n**[${i.id}] ${i.category}** — \`${i.wcag_criterion}\` (ligne ${i.line}, ~${i.fix_time_minutes} min)`);
            lines.push(`${i.description}`);
            if (i.contrast_ratio !== undefined) {
              lines.push(`  Contraste actuel: ${i.contrast_ratio}:1 → requis: ${i.required_ratio}:1`);
            }
          }
          lines.push("");
        }

        if (majIssues.length > 0) {
          lines.push("### 🟡 Issues Majeures");
          for (const i of majIssues) {
            lines.push(`\n**[${i.id}] ${i.category}** — \`${i.wcag_criterion}\` (ligne ${i.line}, ~${i.fix_time_minutes} min)`);
            lines.push(`${i.description}`);
          }
          lines.push("");
        }

        lines.push(`💡 Lance \`/accessibility-fix ${s.name} ios\` pour les corrections code.\n`);
        lines.push("---\n");
      }

      return lines.join("\n");
    },
  },
];
