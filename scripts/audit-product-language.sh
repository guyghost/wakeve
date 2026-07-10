#!/usr/bin/env bash
set -euo pipefail

root="."
mode="all"
while (($#)); do
    case "$1" in
        --fixture-root)
            root="${2:?missing fixture root}"
            shift 2
            ;;
        --catalogs-only)
            mode="catalogs"
            shift
            ;;
        --forbidden-terms-only)
            mode="terms"
            shift
            ;;
        *)
            echo "unknown argument: $1" >&2
            exit 64
            ;;
    esac
done

exec ruby scripts/lib/audit_product_language.rb "$root" "$mode"
