#!/usr/bin/env bash
# Convert the Markdown sources to Word (.docx) with pandoc.
# Author metadata is left empty so the manuscript stays blind for review.
set -euo pipefail
cd "$(dirname "$0")"
for f in 01_abstract 02_manuscript 03_survey_instrument 04_online_appendix 05_cfp_compliance; do
  pandoc "$f.md" --resource-path=. -M author="" -o "$f.docx"
done
echo "Built: $(ls *.docx | tr '\n' ' ')"
