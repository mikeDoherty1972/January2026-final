@echo off
cd /d C:\website\home_app_novemberD_2025
echo START > commit_status.txt
if exist .git (
  set BACKUP=.git_backup_%RANDOM%
  rename .git %BACKUP%
  echo RENAMED .git to %BACKUP% >> commit_status.txt
) else (
  echo NO .git >> commit_status.txt
)
echo GIT INIT >> commit_status.txt
git init >> commit_status.txt 2>&1
git config user.name "mikeDoherty1972"
git config user.email "mike@example.com"
echo GIT ADD >> commit_status.txt
git add -A >> commit_status.txt 2>&1
echo GIT COMMIT >> commit_status.txt
git commit -m "Fresh single-commit snapshot 2025-11-19: full app export" >> commit_status.txt 2>&1 || echo NO_COMMIT >> commit_status.txt
echo LOG >> commit_status.txt
git --no-pager log --oneline -n 5 >> commit_status.txt 2>&1 || echo NO_LOG >> commit_status.txt
echo STATUS >> commit_status.txt
git status --porcelain --untracked-files=all >> commit_status.txt 2>&1
echo DONE >> commit_status.txt

