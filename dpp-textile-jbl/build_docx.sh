#!/usr/bin/env bash
# Convert the Markdown sources to Word (.docx) with pandoc.
set -euo pipefail
cd "$(dirname "$0")"
for f in 01_abstract 02_manuscript 03_survey_instrument 04_online_appendix; do
  pandoc "$f.md" -o "$f.docx"
done
echo "Built: $(ls *.docx | tr '\n' ' ')"
