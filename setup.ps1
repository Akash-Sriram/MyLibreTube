cd "C:\Users\akash\Downloads\Project\Music Management\scratch\MyLibreTube"

Write-Host "Initializing Git Repository..."
git init

Write-Host "Adding official LibreTube as a Git Submodule..."
git submodule add https://github.com/libre-tube/LibreTube.git

Write-Host "Staging our workflow and patch files..."
git add .github/
git add patches/
git add LibreTube/

Write-Host "Committing initial setup..."
git commit -m "Initial commit of custom LibreTube CI pipeline"

Write-Host ""
Write-Host "=========================================================="
Write-Host "SUCCESS! The local repository architecture is fully built."
Write-Host "To push this to your GitHub, run the following commands:"
Write-Host ""
Write-Host "  git remote add origin https://github.com/YourUsername/MyLibreTube.git"
Write-Host "  git branch -M main"
Write-Host "  git push -u origin main"
Write-Host "=========================================================="
