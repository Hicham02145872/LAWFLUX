FROM python:3.12-alpine
WORKDIR /app
COPY infra/discovery/registrar.py /app/registrar.py
USER 65532:65532
CMD ["python", "/app/registrar.py"]
