import os
from google import genai
from google.genai import types
from app.schemas.incident import IncidentProposal
from dotenv import load_dotenv

load_dotenv()  # Loads variables from .env into os.environ
client = genai.Client()

def extract_incident_from_text(alert_text: str) -> IncidentProposal:
    """
    Extract structured DORA RTS metrics from unstructured alert text using Gemini.
    """
    system_instruction = (
        "You are an expert financial IT compliance auditor specialized in EU DORA (Regulation 2022/2554) "
        "and RTS (EU 2024/1772). Analyze the incident alert text and extract quantitative operational metrics. "
        "Be conservative: if client count is ambiguous (e.g. transaction attempts), set affected_clients_count to None. "
        "Provide clear reasoning in extraction_reasoning."
    )
    
    response = client.models.generate_content(
        model="gemini-2.5-flash", 
        contents=alert_text,
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            response_mime_type="application/json",
            response_schema=IncidentProposal,
            temperature=0.1,
        ),
    )

    return response.parsed
