from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import sqlglot
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="OJP SQL Translator")

class TranslationRequest(BaseModel):
    sql: str
    source: str = "oracle"
    target: str = "starrocks"

class TranslationResponse(BaseModel):
    original_sql: str
    translated_sql: str
    source: str
    target: str

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.post("/translate", response_model=TranslationResponse)
def translate_sql(request: TranslationRequest):
    logger.info(f"Received translation request from {request.source} to {request.target}")
    
    try:
        # Use sqlglot to translate
        # Note: sqlglot might not have "starrocks" dialect explicitly, "mysql" or "doris" are usually compatible.
        # StarRocks is MySQL compatible.
        read_dialect = request.source
        write_dialect = request.target
        
        if write_dialect.lower() == "starrocks":
            write_dialect = "starrocks" 
            
        # If sqlglot doesn't support starrocks directly yet in the installed version, fallback to mysql
        # But let's try starrocks first as newer versions support it.
        
        translated = sqlglot.transpile(request.sql, read=read_dialect, write=write_dialect)[0]
        
        return TranslationResponse(
            original_sql=request.sql,
            translated_sql=translated,
            source=request.source,
            target=request.target
        )
    except Exception as e:
        logger.error(f"Translation failed: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Translation failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
