#!/bin/bash

# ==============================================================================
# --- CONFIGURATION: PASTE YOUR JWT TOKEN HERE ---
# ==============================================================================
# To get this, log in on the frontend and copy it from Local Storage.
JWT="eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0YSIsImlhdCI6MTc1MDQwNzUxNiwiZXhwIjoxNzUwNDkzOTE2fQ.N_bdhnmSGlsDo915CcF9sSmKhUsKBPIcszBCQZHpUdktPdIwW2eJcUEt1TctvjTv"


# ==============================================================================
# --- SCRIPT LOGIC (No need to edit below this line) ---
# ==============================================================================
API_URL="http://localhost:8080/api/prompts"
COUNT=50

# --- Sample Data Pools ---
TITLES=(
  "Sci-Fi World Builder" "Fantasy Character Bio" "Technical Blog Post Outline" "Marketing Copy for a SaaS" 
  "Five-Act Story Structure" "Generate a Python Docstring" "Recipe for a Fictional Dish" 
  "Code Review Assistant" "Email to a Potential Investor" "Describe a Renaissance Painting"
  "Dystopian City Description" "Create a Cover Letter" "Brainstorm Startup Ideas" "Explain Quantum Entanglement"
)
CATEGORIES=("Creative Writing" "Productivity" "Business" "Technology" "Education" "Art & Design")
MODELS=("GPT-4" "Claude 3" "DALL-E 3" "Midjourney" "Gemini Pro" "LLaMA 2")
DESCRIPTIONS=(
  "A detailed generator for creating immersive sci-fi settings."
  "Produces rich backstories for D&D or novel characters."
  "Outlines a complete article on a complex technical subject."
  "Generates compelling ad copy for a new software product."
  "Helps writers structure their narrative effectively."
  "Creates PEP 257 compliant docstrings for Python functions."
  "For when you need a recipe that's out of this world."
  "Acts as a second pair of eyes on your pull requests."
  "Crafts a professional and persuasive email."
  "A prompt for generating artistic and historical descriptions."
  "Build a dark and fascinating futuristic cityscape."
  "Tailors a cover letter to a specific job description."
  "Your personal partner for ideation and innovation."
  "Simplifies a mind-bending physics concept for a layperson."
)

echo "Seeding $COUNT prompts to the database..."

for i in $(seq 1 $COUNT)
do
  # --- Select random data for this prompt ---
  rand_title="${TITLES[$RANDOM % ${#TITLES[@]}]} #$i"
  rand_desc="${DESCRIPTIONS[$RANDOM % ${#DESCRIPTIONS[@]}]}"
  rand_model="${MODELS[$RANDOM % ${#MODELS[@]}]}"
  rand_category="${CATEGORIES[$RANDOM % ${#CATEGORIES[@]}]}"
  
  # --- Construct the JSON payload ---
  json_payload=$(cat <<EOF
{
  "title": "$rand_title",
  "description": "$rand_desc",
  "text": "You are a helpful assistant. The user wants to generate content based on the title: '$rand_title'. Please provide a detailed and high-quality response that fits the category of '$rand_category'.",
  "model": "$rand_model",
  "category": "$rand_category"
}
EOF
)

  # --- Make the API call with curl ---
  response_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT" \
    -d "$json_payload")

  if [ "$response_code" -eq 201 ]; then
    echo "  ($i/$COUNT) Successfully created prompt: '$rand_title'"
  else
    echo "  ($i/$COUNT) FAILED to create prompt. Server responded with code: $response_code"
  fi

  # Be nice to the server, wait a tiny bit between requests
  sleep 0.05
done

echo "Seeding complete."