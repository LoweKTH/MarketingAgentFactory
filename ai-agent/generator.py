from transformers import pipeline

# Separata pipelines för olika syften
generate_pipe = pipeline("text2text-generation", model="google/flan-t5-base", temperature=1.2)
evaluate_pipe = pipeline("text2text-generation", model="google/flan-t5-base", temperature=1.0)


#hjälpmetod till feedback loop
def self_evaluate(text: str) -> str:
    prompt = f"Is the following text good? Answer yes or no and explain:\n\n{text}"
    return evaluate_pipe(prompt, max_new_tokens=60)[0]["generated_text"]

#hjälpmetod till feedback loop
def improve_text(text: str) -> str:
    prompt = f"Improve the following text:\n\n{text}"
    return generate_pipe(prompt, max_new_tokens=100)[0]["generated_text"]

def generate_with_feedback_loop(prompt: str, max_iterations: int = 3) -> dict:
    history = []

    current_text = generate_pipe(prompt, max_new_tokens=100)[0]["generated_text"]

    for i in range(max_iterations):
        evaluation = self_evaluate(current_text)
        history.append({
            "iteration": i + 1,
            "text": current_text,
            "evaluation": evaluation
        })

        if "yes" in evaluation.lower():
            return {
                "final_text": current_text,
                "iterations": history
            }

        current_text = improve_text(current_text)

    # Om ingen iteration blev godkänd
    return {
        "final_text": current_text,
        "iterations": history,
        "note": "Returned after max iterations"
    }
