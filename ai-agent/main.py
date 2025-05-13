#main.py är startpunkten för hela agenten
from fastapi import FastAPI

#importerar vår definierade router från routes.py
from routes import router

#Skapar själva FastAPI-applikationen
app = FastAPI()

#Monterar dina endpoints från routes.py.
app.include_router(router)

