# MomoTerminal Dangerfile
# https://danger.systems/ruby/

# Warn when PR doesn't have a description
warn("Please provide a description for your PR.") if github.pr_body.length < 10

# Warn when PR is too large
warn("This PR is quite big. Consider breaking it into smaller, focused PRs.") if git.lines_of_code > 500

# Warn when there are changes to build files
warn("Changes to build.gradle.kts files detected. Please ensure CI/CD pipelines are tested.") if git.modified_files.any? { |file| file.include?("build.gradle.kts") }

# Ensure all PRs have labels
warn("This PR doesn't have any labels. Please add appropriate labels.") if github.pr_labels.empty?

# Warn about WIP PRs
warn("This PR is marked as Work In Progress") if github.pr_title.downcase.include?("[wip]") || github.pr_title.downcase.include?("wip:")

# Check for files that should not be committed
["*.keystore", "google-services.json", "local.properties"].each do |pattern|
  if git.modified_files.any? { |file| File.fnmatch(pattern, File.basename(file)) }
    fail("#{pattern} files should not be committed!")
  end
end

# Check for TODO/FIXME comments in new code
(git.modified_files + git.added_files).each do |file|
  next unless file.end_with?(".kt", ".java")
  next unless File.exist?(file)
  
  File.readlines(file).each_with_index do |line, index|
    if line.include?("TODO") || line.include?("FIXME")
      warn("#{file}:#{index + 1} contains TODO/FIXME comment", file: file, line: index + 1)
    end
  end
end

# Android Lint integration
android_lint.gradle_task = "lintDebug"
android_lint.filtering = true
android_lint.skip_gradle_task = false
android_lint.report_file = "app/build/reports/lint-results-debug.xml"
android_lint.lint(inline_mode: true)

# Summary of changes
message("**Changes Summary:**")
message("- #{git.added_files.count} file(s) added")
message("- #{git.modified_files.count} file(s) modified")
message("- #{git.deleted_files.count} file(s) deleted")
message("- #{git.lines_of_code} lines changed")

# Check for test files
modified_kt_files = git.modified_files.select { |file| file.end_with?(".kt") && !file.include?("Test") }
if modified_kt_files.any? && !git.modified_files.any? { |file| file.include?("Test.kt") }
  warn("You have modified Kotlin files but haven't updated any tests. Please consider adding tests.")
end

# Congrats message for maintainers
if github.pr_author == "dependabot[bot]"
  message("Thanks Dependabot! 🤖")
end
