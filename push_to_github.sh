#!/data/data/com.termux/files/usr/bin/bash
# Termux me chalayein:  bash push_to_github.sh
set -e
pkg install -y git >/dev/null
echo "GitHub username:"; read GH_USER
echo "Repo ka naam (khali repo pehle github.com par bana lein, jaise drugsearch):"; read GH_REPO
echo "GitHub token (Settings > Developer settings > Tokens, 'repo' permission):"; read -s GH_TOKEN
git init -q
git checkout -q -B main
git add -A
git -c user.name="$GH_USER" -c user.email="$GH_USER@users.noreply.github.com" commit -q -m "Drug search app"
git remote remove origin 2>/dev/null || true
git remote add origin "https://$GH_USER:$GH_TOKEN@github.com/$GH_USER/$GH_REPO.git"
git push -u origin main --force
echo
echo "Ho gaya. github.com/$GH_USER/$GH_REPO > Actions me 'Build APK' ke bahar aane ka 5-10 minute intezaar karein."
echo "Phir us run ke andar 'DrugSearch-APK' download karein."
