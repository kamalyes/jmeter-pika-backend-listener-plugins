from fastapi import FastAPI
import uvicorn
import random
app = FastAPI()

@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.get("/api/test")
def read_item(keyword: str = None):
    return {"keyword": f'{keyword}-{random.randint(1,155555)}'}


if __name__ == "__main__":
    uvicorn.run(
        app="backend:app",
        host='127.0.0.1',
        port=8000,
        reload=True,
    )
