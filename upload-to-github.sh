#!/bin/bash
set -e

REPO_URL="https://github.com/jacobgantzpolyglot/Arceeus-CD-Timer.git"
COMMIT_MSG="${1:-Update Arceuus cooldown timer plugin}"

cd "$HOME/arceuus-buff-overlay"

cat > .gitignore <<'GITIGNORE'
.gradle/
build/
out/
.idea/
*.iml
*.class
*.log
credentials.properties
*.properties.bak
GITIGNORE

if [ ! -d .git ]; then
  git init
fi

git branch -M main

if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

git add .

if git diff --cached --quiet; then
  echo "No changes to commit."
else
  git commit -m "$COMMIT_MSG"
fi

echo
echo "Trying normal push first..."
if git push -u origin main; then
  echo "Uploaded successfully."
else
  echo
  echo "Normal push failed, probably because the GitHub repo already has a README commit."
  echo "If you want your local plugin folder to replace what is on GitHub, run:"
  echo
  echo "  git push -u origin main --force-with-lease"
  echo
  echo "Use force-with-lease only if you're okay overwriting the current GitHub repo contents."
fi
