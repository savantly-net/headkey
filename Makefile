export PROJECT_DIR = $(shell pwd)

-include local.mk

.DEFAULT_GOAL := help

.PHONY: help
help:
	@echo "Available make targets:"
	@echo "  help           - Show this help message"
	@echo "  compile        - Compile all projects"
	@echo "  test           - Run all tests across all projects"
	@echo "  publish-local  - Build and publish to local repository"
	@echo "  publish        - Build and publish release"
	@echo "  release        - Create and push a new release tag"
	@echo ""

SNAPSHOT_VERSION := $(shell cat VERSION)
# Strip -SNAPSHOT from version
FINAL_VERSION := $(shell echo $(SNAPSHOT_VERSION) | sed 's/-SNAPSHOT//')
NEXT_VERSION := $(shell echo $(FINAL_VERSION) | awk -F. '{$$NF = $$NF + 1;} 1' | sed 's/ /./g')-SNAPSHOT
GIT_COMMIT := $(shell git rev-parse --short HEAD)


.PHONY: run-postgres
run-postgres:
	@echo "Running app with postgres..."
	docker compose --profile postgres up -d
	./gradlew rest:quarkusDev -Dquarkus.profile=prod
	docker compose --profile postgres down

.PHONY: run-elasticsearch
run-elasticsearch:
	@echo "Running app with Elasticsearch..."
	docker compose --profile elasticsearch up -d
	./gradlew rest:quarkusDev -Dquarkus.profile=elasticsearch
	docker compose --profile elasticsearch down

.PHONY: run
run: run-elasticsearch

.PHONY: dev
dev: run


.PHONY: test
test:
	@echo "Running all tests..."
	./gradlew test
	@echo "All tests completed."

.PHONY: test-debug
test-debug:
	@echo "Running all tests..."
	./gradlew test --debug
	@echo "All tests completed."

.PHONY: test-quarkus
test-quarkus:
	@echo "Running Quarkus integration tests..."
	./gradlew :rest:test
	@echo "Quarkus tests completed."

.PHONY: compile
compile:
	@echo "Compiling all projects..."
	./gradlew compileJava
	@echo "Compilation completed."

.PHONY publish-local:
publish-local:
	@echo "Preparing release..."
	./gradlew clean build
	@echo "Building release..."
	./gradlew publish
	@echo "Local Release published successfully."

.PHONY publish:
publish: publish-local
	@echo "Releasing..."
	./gradlew jreleaserFullRelease --stacktrace
	@echo "Release completed successfully."

.PHONY: ensure-git-repo-pristine
ensure-git-repo-pristine:
	@echo "Ensuring git repo is pristine"
	@[[ $(shell git status --porcelain=v1 2>/dev/null | wc -l) -gt 0 ]] && echo "Git repo is not pristine" && exit 1 || echo "Git repo is pristine"

.PHONY: bump-version
bump-version:
	@echo "Bumping version to $(NEXT_VERSION)"
	@echo $(NEXT_VERSION) > VERSION
	git add VERSION
	git commit -m "Published $(FINAL_VERSION) and prepared for $(NEXT_VERSION)"

.PHONY: tag-version
tag-version:
	@echo "Preparing release..."
	@echo "Version: $(FINAL_VERSION)"
	@echo "Commit: $(GIT_COMMIT)"
	@echo $(FINAL_VERSION) > VERSION
	git add VERSION
	git commit -m "Published $(FINAL_VERSION)"
	git tag -a $(FINAL_VERSION) -m "Release $(FINAL_VERSION)"
	git push origin $(FINAL_VERSION)
	@echo "Tag $(FINAL_VERSION) created and pushed to origin"

.PHONY: release
release: ensure-git-repo-pristine tag-version bump-version
	git push
	@echo "Release $(VERSION) completed and pushed to origin"
