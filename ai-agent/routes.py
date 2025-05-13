from fastapi import APIRouter
from pydantic import BaseModel

# Importerar vår textgenereringsfunktion från generator.py
from generator import generate_with_feedback_loop

# Skapar ett APIRouter-objekt så att denna modul kan monteras i main.py
router = APIRouter()

# Definierar datamodellen för inkommande JSON-data till endpointen
# Den innehåller bara ett fält: "topic" (sträng)
class TopicRequest(BaseModel):
    topic: str
    max_iterations: int = 3 #valfri, default = 3

# Definierar POST-endpointen /generate
# Användaren skickar in JSON: { "topic": "valfritt ämne" }
# Returnerar: { "text": "genererad text..." }


@router.post("/generate/loop")
def generate_with_loop(request: TopicRequest):
    result = generate_with_feedback_loop(
        prompt=request.topic,
        max_iterations=request.max_iterations
    )
    return result
