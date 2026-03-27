#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="elastic-autograder"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
KIND_CONFIG="${REPO_ROOT}/k8s/kind-config.yaml"
IMAGE_BUILD_ROOT="${REPO_ROOT}/backend/grading/image-build"

echo "Checking required tools..."
command -v kind >/dev/null 2>&1 || { echo "Error: kind is not installed."; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "Error: docker is not installed."; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo "Error: kubectl is not installed."; exit 1; }

if [[ ! -f "${KIND_CONFIG}" ]]; then
  echo "Error: kind config file not found at ${KIND_CONFIG}"
  exit 1
fi

if [[ ! -d "${IMAGE_BUILD_ROOT}" ]]; then
  echo "Error: image build directory not found at ${IMAGE_BUILD_ROOT}"
  exit 1
fi

echo "Checking for kind cluster '${CLUSTER_NAME}'..."
if ! kind get clusters | grep -xq "${CLUSTER_NAME}"; then
  echo "Cluster not found. Creating from config..."
  kind create cluster --config "${KIND_CONFIG}"
else
  echo "Cluster '${CLUSTER_NAME}' already exists."
fi

echo "Changing to image build directory..."
cd "${IMAGE_BUILD_ROOT}"

echo "Building Fibonacci grader image..."
docker build --no-cache \
  -f runtime/Dockerfile \
  -t ea-grader-fibbonaci:v1 \
  --build-arg GRADER_NAME=fibbonaci \
  .

echo "Building Two Sum grader image..."
docker build --no-cache \
  -f runtime/Dockerfile \
  -t ea-grader-twosum:v1 \
  --build-arg GRADER_NAME=twosum \
  .

echo "Loading grader images into kind cluster..."
kind load docker-image ea-grader-fibbonaci:v1 --name "${CLUSTER_NAME}"
kind load docker-image ea-grader-twosum:v1 --name "${CLUSTER_NAME}"

echo "Verifying cluster context..."
kubectl cluster-info --context "kind-${CLUSTER_NAME}"

echo "Setup complete."
echo "Cluster '${CLUSTER_NAME}' is ready and grader images are loaded."