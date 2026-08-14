#!/bin/bash

export SPRING_PROFILES_ACTIVE=test
export DB_URL="jdbc:mysql://127.0.0.1:3308/nguon_goc_so_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
export DB_USERNAME="nguongocso_test"
export DB_PASSWORD="Dev2026"

./mvnw clean test