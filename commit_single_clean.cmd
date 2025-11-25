@echo off
cd /d C:\website\home_app_novemberD_2025
set LOG=commit_single_output.txt
echo START > %LOG%
if exist .git (
  set BACKUP=.git_backup_%RANDOM%
  ren .git %BACKUP%
  echo RENAMED .git to %BACKUP% >> %LOG%
) else (
  echo NO .git >> %LOG%
)
echo REMOVING any leftover .git >> %LOG%
if exist .git rmdir /s /q .git >> %LOG% 2>&1
echo GIT INIT >> %LOG%
git init >> %LOG% 2>&1
rem set a safe user name/email without special characters
git config user.name MikeDoherty1972
git config user.email mike@example.com
echo GIT ADD >> %LOG%
git add -A >> %LOG% 2>&1
echo GIT COMMIT >> %LOG%
git commit -m "Fresh single-commit snapshot 2025-11-19: full app export" --author="Mike Doherty <mike@example.com>" >> %LOG% 2>&1 || echo COMMIT_FAILED >> %LOG%
echo REV_LIST_COUNT >> %LOG%
git rev-list --count HEAD >> %LOG% 2>&1 || echo NO_HEAD >> %LOG%
echo LOG >> %LOG%
git --no-pager log --oneline -n 5 >> %LOG% 2>&1 || echo NO_LOG >> %LOG%
echo STATUS >> %LOG%
git status --porcelain --untracked-files=all >> %LOG% 2>&1
echo DONE >> %LOG%

