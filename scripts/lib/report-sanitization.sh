#!/usr/bin/env bash

sanitize_report_file() {
    local report="$1"

    [ -n "$report" ] && [ -f "$report" ] || return 0
    perl -pi -e 'BEGIN { $home = $ENV{"HOME"} // ""; $home = quotemeta($home); } s/\r//g; s/[ \t]+$//; s/$home/~/g if $home ne "";' "$report"
}
