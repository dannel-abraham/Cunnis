"""
CUNNIS BACKEND - cu.dandroid.cunnis
Backend profesional para gestión de granja cunícola (offline-first).
Autor: Dannel Alvarez Nieto | Telegram: @alvareznieto.dannel | Email: alvareznieto.dannel@gmail.com
Dedicatoria: "a mi viejo"
"""

import os
import sqlite3
import json
import base64
import uuid
import shutil
import re
from datetime import datetime, timedelta, date
from flask import Flask, request, jsonify, g, send_file
from werkzeug.utils import secure_filename

# ─────────────────────────────────────────────────────────────────────────────
# CONFIGURACIÓN
# ─────────────────────────────────────────────────────────────────────────────
app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 50 * 1024 * 1024  # 50MB para imágenes base64
DB_PATH = "cunnis.db"
BACKUP_DIR = "backups"
ICONS_DIR = "assets/icons"
os.makedirs(BACKUP_DIR, exist_ok=True)
os.makedirs(ICONS_DIR, exist_ok=True)

# Paleta de colores oficial
APP_PALETTE = {
    "accent": "#FF8A65",
    "primary": "#80CBC4",
    "primaryDark": "#4DB6AC",
    "controlHighlight": "#A7FFEB",
    "controlNormal": "#00BFA5"
}

# Umbrales por defecto (días/meses) - configurables vía API
DEFAULT_SETTINGS = {
    "female_reproductive_age_months": 5,
    "male_reproductive_age_months": 6,
    "weaning_age_days": 30,
    "gestation_days": 31,
    "alert_days_before": 3,
    "notification_hour_start": 6,
    "notification_hour_end": 22,
    "weight_unit": "lb",
    "date_format": "YYYY-MM-DD"
}
# ─────────────────────────────────────────────────────────────────────────────
# BASE DE DATOS
# ─────────────────────────────────────────────────────────────────────────────
def get_db():
    if 'db' not in g:
        g.db = sqlite3.connect(DB_PATH, check_same_thread=False)
        g.db.row_factory = sqlite3.Row
        g.db.execute("PRAGMA journal_mode=WAL;")
        g.db.execute("PRAGMA foreign_keys=ON;")
        g.db.execute("PRAGMA busy_timeout=5000;")  # Manejo de concurrencia
    return g.db

@app.teardown_appcontext
def close_db(exception):
    db = g.pop('db', None)
    if db:
        db.close()

def init_db():
    """Inicializa la BD con todas las tablas, índices y datos por defecto"""
    with app.app_context():
        db = get_db()
        db.executescript("""
            -- Configuración global
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY, 
                value TEXT
            );
            
            -- Jaulas
            CREATE TABLE IF NOT EXISTS cages (
                id TEXT PRIMARY KEY,
                number INTEGER UNIQUE,
                type TEXT DEFAULT 'standard' CHECK(type IN ('standard', 'cemental', 'maternity')),
                capacity INTEGER DEFAULT 1,
                is_active INTEGER DEFAULT 1,
                created_at TEXT DEFAULT (datetime('now')),
                notes TEXT
            );
            
            -- Conejos
            CREATE TABLE IF NOT EXISTS rabbits (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                gender TEXT CHECK(gender IN ('male', 'female', 'unknown')) NOT NULL,
                breed TEXT DEFAULT 'Mixed',
                birth_date TEXT,
                cage_id TEXT,
                sire_id TEXT,                dam_id TEXT,
                status TEXT DEFAULT 'alive' CHECK(status IN ('alive', 'dead', 'sold', 'retired', 'transferred')),
                is_reproductive INTEGER DEFAULT 0,
                created_at TEXT DEFAULT (datetime('now')),
                updated_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY(cage_id) REFERENCES cages(id) ON DELETE SET NULL,
                FOREIGN KEY(sire_id) REFERENCES rabbits(id) ON DELETE SET NULL,
                FOREIGN KEY(dam_id) REFERENCES rabbits(id) ON DELETE SET NULL
            );
            
            -- Historial de peso (con foto mensual)
            CREATE TABLE IF NOT EXISTS weight_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rabbit_id TEXT NOT NULL,
                weight_lb REAL NOT NULL,
                date TEXT NOT NULL,
                photo_base64 TEXT,
                notes TEXT,
                created_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY(rabbit_id) REFERENCES rabbits(id) ON DELETE CASCADE
            );
            
            -- Historial de salud
            CREATE TABLE IF NOT EXISTS health_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rabbit_id TEXT NOT NULL,
                condition_type TEXT,
                diagnosis TEXT,
                treatment TEXT,
                vet_name TEXT,
                date TEXT NOT NULL,
                notes TEXT,
                photo_base64 TEXT,
                FOREIGN KEY(rabbit_id) REFERENCES rabbits(id) ON DELETE CASCADE
            );
            
            -- Historial reproductivo: Montas
            CREATE TABLE IF NOT EXISTS mating_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                female_id TEXT NOT NULL,
                male_id TEXT NOT NULL,
                date TEXT NOT NULL,
                effective INTEGER DEFAULT 0,
                notes TEXT,
                created_at TEXT DEFAULT (datetime('now')),
                FOREIGN KEY(female_id) REFERENCES rabbits(id) ON DELETE CASCADE,
                FOREIGN KEY(male_id) REFERENCES rabbits(id) ON DELETE CASCADE
            );
            
            -- Historial reproductivo: Partos            CREATE TABLE IF NOT EXISTS litter_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                mating_id INTEGER,
                birth_date TEXT NOT NULL,
                total_born INTEGER DEFAULT 0,
                live_born INTEGER DEFAULT 0,
                dead_born INTEGER DEFAULT 0,
                weaned_count INTEGER DEFAULT 0,
                notes TEXT,
                FOREIGN KEY(mating_id) REFERENCES mating_logs(id) ON DELETE SET NULL
            );
            
            -- Alertas persistentes
            CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rabbit_id TEXT,
                alert_type TEXT CHECK(alert_type IN (
                    'reproductive_age', 'heat_cycle', 'gestation_due', 
                    'weaning_due', 'weight_anomaly', 'health_reminder', 'cage_capacity'
                )),
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                priority TEXT DEFAULT 'medium' CHECK(priority IN ('low', 'medium', 'high')),
                triggered_at TEXT DEFAULT (datetime('now')),
                acknowledged INTEGER DEFAULT 0,
                resolved_at TEXT,
                metadata TEXT,
                FOREIGN KEY(rabbit_id) REFERENCES rabbits(id) ON DELETE CASCADE
            );
            
            -- Índices para optimizar consultas frecuentes
            CREATE INDEX IF NOT EXISTS idx_rabbits_cage ON rabbits(cage_id);
            CREATE INDEX IF NOT EXISTS idx_rabbits_gender_status ON rabbits(gender, status);
            CREATE INDEX IF NOT EXISTS idx_rabbits_birth ON rabbits(birth_date);
            CREATE INDEX IF NOT EXISTS idx_weight_logs_rabbit_date ON weight_logs(rabbit_id, date);
            CREATE INDEX IF NOT EXISTS idx_health_logs_rabbit_date ON health_logs(rabbit_id, date);
            CREATE INDEX IF NOT EXISTS idx_mating_female_date ON mating_logs(female_id, date);
            CREATE INDEX IF NOT EXISTS idx_litter_mating ON litter_logs(mating_id);
            CREATE INDEX IF NOT EXISTS idx_alerts_active ON alerts(acknowledged, priority);
        """)
        
        # Insertar settings por defecto si está vacío
        cursor = db.execute("SELECT COUNT(*) FROM settings")
        if cursor.fetchone()[0] == 0:
            for k, v in DEFAULT_SETTINGS.items():
                db.execute("INSERT INTO settings (key, value) VALUES (?, ?)", (k, str(v)))
        
        # Crear jaula cemental por defecto si no existe
        db.execute("""
            INSERT OR IGNORE INTO cages (id, number, type, capacity, notes)             VALUES ('CEMENTAL', 0, 'cemental', 1, 'Conejo reproductor principal')
        """)
        
        db.commit()

# ─────────────────────────────────────────────────────────────────────────────
# UTILIDADES
# ─────────────────────────────────────────────────────────────────────────────
def generate_rabbit_id():
    """Genera ID único de 6 caracteres para conejos: RJ + secuencia"""
    db = get_db()
    count = db.execute("SELECT COUNT(*) FROM rabbits").fetchone()[0]
    return f"RJ{count + 1:04d}"[:6]

def generate_cage_id(cage_type, number):
    """Genera ID para jaulas: CA### para standard, CE### para cemental, etc."""
    prefix = {'standard': 'CA', 'cemental': 'CE', 'maternity': 'CM'}.get(cage_type, 'CA')
    return f"{prefix}{number:03d}"[:6]

def parse_date(date_str):
    """Valida y parsea fecha en formato YYYY-MM-DD"""
    if not date_str:
        return None
    try:
        return datetime.strptime(date_str, "%Y-%m-%d").date()
    except ValueError:
        raise ValueError(f"Formato de fecha inválido: '{date_str}'. Use YYYY-MM-DD")

def is_notification_time():
    """Verifica si la hora actual está dentro del rango de notificaciones"""
    settings = {r['key']: r['value'] for r in get_db().execute("SELECT key, value FROM settings")}
    start = int(settings.get("notification_hour_start", 6))
    end = int(settings.get("notification_hour_end", 22))
    now = datetime.now().hour
    return start <= now <= end

def row_to_dict(row):
    """Convierte sqlite3.Row a dict, manejando valores NULL"""
    if not row:
        return None
    return {key: (None if row[key] is None else row[key]) for key in row.keys()}

def rows_to_list(cursor):
    """Convierte resultado de consulta a lista de dicts"""
    return [row_to_dict(r) for r in cursor.fetchall()]

def load_launcher_icon_base64():
    """Carga ic_launcher.png y lo convierte a base64 para incluir en /info"""
    icon_path = os.path.join(ICONS_DIR, "ic_launcher.png")
    if os.path.exists(icon_path):        with open(icon_path, "rb") as f:
            return base64.b64encode(f.read()).decode('utf-8')
    return None  # Si no existe, se omite

# ─────────────────────────────────────────────────────────────────────────────
# MIDDLEWARE: CORS & HEADERS
# ─────────────────────────────────────────────────────────────────────────────
@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, PUT, DELETE, OPTIONS'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type, Authorization, X-Requested-With'
    response.headers['Access-Control-Expose-Headers'] = 'Content-Length, X-Content-Range'
    return response

# ─────────────────────────────────────────────────────────────────────────────
# RUTAS API - CORE
# ─────────────────────────────────────────────────────────────────────────────

@app.route('/api/v1/health', methods=['GET'])
def health():
    """Endpoint de salud del servicio"""
    return jsonify({
        "status": "ok", 
        "db_path": DB_PATH, 
        "timestamp": datetime.now().isoformat(),
        "version": "0.1.0.0"
    })

@app.route('/api/v1/info', methods=['GET'])
def info():
    """Información de la aplicación + icono launcher en base64"""
    return jsonify({
        "app": "Cunnis",
        "version": "0.1.0.0",
        "package": "cu.dandroid.cunnis",
        "author": "Dannel Alvarez Nieto",
        "telegram": "@alvareznieto.dannel",
        "email": "alvareznieto.dannel@gmail.com",
        "dedication": "a mi viejo",
        "palette": APP_PALETTE,
        "launcher_icon_base64": load_launcher_icon_base64(),
        "features": [
            "offline-first",
            "base64-images",
            "alert-system",
            "export-import",
            "imperial-units"
        ]
    })
# ───────────── Jaulas ─────────────
@app.route('/api/v1/cages', methods=['GET'])
def get_cages():
    """Lista todas las jaulas activas"""
    cage_type = request.args.get('type')
    query = "SELECT * FROM cages WHERE is_active=1"
    params = []
    if cage_type:
        query += " AND type=?"
        params.append(cage_type)
    query += " ORDER BY number"
    return jsonify(rows_to_list(get_db().execute(query, params)))

@app.route('/api/v1/cages', methods=['POST'])
def create_cage():
    """Crea una nueva jaula"""
    data = request.json
    if not data.get('number') or not data.get('type'):
        return jsonify({"error": "Faltan campos: number, type"}), 400
    
    cage_type = data['type']
    if cage_type not in ('standard', 'cemental', 'maternity'):
        return jsonify({"error": "Tipo de jaula inválido"}), 400
    
    cid = generate_cage_id(cage_type, data['number'])
    
    try:
        get_db().execute(
            "INSERT INTO cages (id, number, type, capacity, notes) VALUES (?, ?, ?, ?, ?)",
            (cid, data['number'], cage_type, data.get('capacity', 1), data.get('notes'))
        )
        get_db().commit()
        return jsonify({"id": cid, "message": "Jaula creada"}), 201
    except sqlite3.IntegrityError:
        return jsonify({"error": "Número de jaula duplicado"}), 409

@app.route('/api/v1/cages/<cage_id>', methods=['GET', 'PUT', 'DELETE'])
def manage_cage(cage_id):
    """Operaciones CRUD sobre una jaula específica"""
    db = get_db()
    
    if request.method == 'GET':
        cage = row_to_dict(db.execute("SELECT * FROM cages WHERE id=?", (cage_id,)).fetchone())
        return jsonify(cage) if cage else (jsonify({"error": "Jaula no encontrada"}), 404)
    
    elif request.method == 'PUT':
        data = request.json
        db.execute(
            "UPDATE cages SET type=?, capacity=?, notes=?, is_active=? WHERE id=?",            (data.get('type'), data.get('capacity'), data.get('notes'), 
             data.get('is_active', 1), cage_id)
        )
        db.commit()
        return jsonify({"message": "Jaula actualizada"})
    
    elif request.method == 'DELETE':
        # Soft delete: desactivar en lugar de eliminar
        db.execute("UPDATE cages SET is_active=0 WHERE id=?", (cage_id,))
        db.commit()
        return jsonify({"message": "Jaula desactivada"})

# ───────────── Conejos ─────────────
@app.route('/api/v1/rabbits', methods=['GET'])
def get_rabbits():
    """Lista conejos con filtros opcionales"""
    filters = {
        'gender': request.args.get('gender'),
        'cage_id': request.args.get('cage_id'),
        'status': request.args.get('status', 'alive'),
        'breed': request.args.get('breed'),
        'min_age_days': request.args.get('min_age_days', type=int),
        'max_age_days': request.args.get('max_age_days', type=int)
    }
    
    query = "SELECT * FROM rabbits WHERE 1=1"
    params = []
    
    for key, value in filters.items():
        if value is not None and key not in ('min_age_days', 'max_age_days'):
            query += f" AND {key}=?"
            params.append(value)
    
    # Filtro por edad calculada
    if filters['min_age_days'] or filters['max_age_days']:
        query += """ AND (
            julianday('now') - julianday(birth_date) BETWEEN ? AND ?
        )"""
        params.extend([filters['min_age_days'] or 0, filters['max_age_days'] or 99999])
    
    query += " ORDER BY name"
    return jsonify(rows_to_list(get_db().execute(query, params)))

@app.route('/api/v1/rabbits', methods=['POST'])
def create_rabbit():
    """Registra un nuevo conejo"""
    data = request.json
    required = ['name', 'gender']
    if not all(k in data for k in required):
        return jsonify({"error": f"Campos requeridos: {required}"}), 400    
    # Validar fecha de nacimiento si se proporciona
    if data.get('birth_date'):
        parse_date(data['birth_date'])
    
    # Validar que padres existan si se referencian
    db = get_db()
    for parent_key in ['sire_id', 'dam_id']:
        if data.get(parent_key):
            exists = db.execute("SELECT 1 FROM rabbits WHERE id=?", (data[parent_key],)).fetchone()
            if not exists:
                return jsonify({"error": f"{parent_key} no existe: {data[parent_key]}"}), 400
    
    rid = generate_rabbit_id()
    
    try:
        db.execute("""
            INSERT INTO rabbits (
                id, name, gender, breed, birth_date, cage_id, 
                sire_id, dam_id, status, is_reproductive
            ) VALUES (?,?,?,?,?,?,?,?,?,?)
        """, (
            rid, data['name'], data['gender'], data.get('breed', 'Mixed'),
            data.get('birth_date'), data.get('cage_id'),
            data.get('sire_id'), data.get('dam_id'),
            'alive', int(data.get('is_reproductive', False))
        ))
        db.commit()
        return jsonify({"id": rid, "message": "Conejo registrado"}), 201
    except sqlite3.IntegrityError as e:
        return jsonify({"error": "Error de integridad: " + str(e)}), 400

@app.route('/api/v1/rabbits/<rid>', methods=['GET'])
def get_rabbit(rid):
    """Obtiene datos completos de un conejo + historiales resumidos"""
    db = get_db()
    rabbit = row_to_dict(db.execute("SELECT * FROM rabbits WHERE id=?", (rid,)).fetchone())
    if not rabbit:
        return jsonify({"error": "Conejo no encontrado"}), 404
    
    # Añadir estadísticas rápidas
    rabbit['weight_count'] = db.execute(
        "SELECT COUNT(*) FROM weight_logs WHERE rabbit_id=?", (rid,)
    ).fetchone()[0]
    rabbit['health_count'] = db.execute(
        "SELECT COUNT(*) FROM health_logs WHERE rabbit_id=?", (rid,)
    ).fetchone()[0]
    rabbit['mating_count'] = db.execute(
        "SELECT COUNT(*) FROM mating_logs WHERE female_id=? OR male_id=?", (rid, rid)
    ).fetchone()[0]    
    return jsonify(rabbit)

@app.route('/api/v1/rabbits/<rid>', methods=['PUT'])
def update_rabbit(rid):
    """Actualiza datos de un conejo existente"""
    data = request.json
    allowed_fields = ['name', 'breed', 'cage_id', 'sire_id', 'dam_id', 'status', 'birth_date', 'is_reproductive']
    updates = []
    values = []
    
    for field in allowed_fields:
        if field in 
            if field == 'birth_date' and data[field]:
                parse_date(data[field])  # Validar formato
            updates.append(f"{field}=?")
            values.append(data[field])
    
    if not updates:
        return jsonify({"error": "No hay campos válidos para actualizar"}), 400
    
    updates.append("updated_at=datetime('now')")
    values.append(rid)
    
    db = get_db()
    db.execute(f"UPDATE rabbits SET {','.join(updates)} WHERE id=?", values)
    db.commit()
    return jsonify({"message": "Conejo actualizado", "id": rid})

@app.route('/api/v1/rabbits/<rid>', methods=['DELETE'])
def delete_rabbit(rid):
    """Elimina lógicamente un conejo (cambia status a 'dead')"""
    db = get_db()
    # Verificar si tiene descendencia
    has_offspring = db.execute(
        "SELECT 1 FROM rabbits WHERE sire_id=? OR dam_id=? LIMIT 1", (rid, rid)
    ).fetchone()
    
    if has_offspring:
        # No eliminar, solo cambiar status
        db.execute("UPDATE rabbits SET status='dead', updated_at=datetime('now') WHERE id=?", (rid,))
        db.commit()
        return jsonify({"message": "Conejo marcado como fallecido (tiene descendencia)"})
    else:
        # Eliminación física segura
        db.execute("DELETE FROM rabbits WHERE id=?", (rid,))
        db.commit()
        return jsonify({"message": "Conejo eliminado"})

# ───────────── Historiales ─────────────def create_log_endpoint(table_name, rabbit_col='rabbit_id', extra_validation=None):
    """Factory para endpoints de historiales"""
    def add_log(rid):
        data = request.json.copy()
        data['date'] = data.get('date', date.today().isoformat())
        parse_date(data['date'])  # Validar fecha
        
        # Validación personalizada si se proporciona
        if extra_validation:
            error = extra_validation(data)
            if error:
                return jsonify({"error": error}), 400
        
        # Preparar inserción
        cols = [rabbit_col] + [k for k in data.keys() if k != 'id']
        vals = [rid] + [data[k] for k in cols[1:]]
        placeholders = ','.join(['?' for _ in cols])
        
        db = get_db()
        db.execute(f"INSERT INTO {table_name} ({','.join(cols)}) VALUES ({placeholders})", vals)
        db.commit()
        
        log_id = db.execute("SELECT last_insert_rowid()").fetchone()[0]
        return jsonify({"id": log_id, "message": f"Registro añadido a {table_name}"}), 201
    
    return add_log

# Endpoints de historiales
app.add_url_rule('/api/v1/logs/weight/<rid>', 'add_weight', 
                 create_log_endpoint('weight_logs'), methods=['POST'])
app.add_url_rule('/api/v1/logs/health/<rid>', 'add_health', 
                 create_log_endpoint('health_logs'), methods=['POST'])

@app.route('/api/v1/logs/weight/<rid>', methods=['GET'])
def get_weight_logs(rid):
    """Historial de peso con soporte para paginación"""
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 12, type=int)
    offset = (page - 1) * per_page
    
    db = get_db()
    total = db.execute(
        "SELECT COUNT(*) FROM weight_logs WHERE rabbit_id=?", (rid,)
    ).fetchone()[0]
    
    logs = rows_to_list(db.execute("""
        SELECT id, weight_lb, date, notes, created_at 
        FROM weight_logs 
        WHERE rabbit_id=? 
        ORDER BY date DESC         LIMIT ? OFFSET ?
    """, (rid, per_page, offset)))
    
    return jsonify({
        "rabbit_id": rid,
        "logs": logs,
        "pagination": {
            "page": page,
            "per_page": per_page,
            "total": total,
            "pages": (total + per_page - 1) // per_page
        }
    })

@app.route('/api/v1/logs/health/<rid>', methods=['GET'])
def get_health_logs(rid):
    """Historial de salud"""
    return jsonify(rows_to_list(get_db().execute("""
        SELECT * FROM health_logs 
        WHERE rabbit_id=? 
        ORDER BY date DESC
    """, (rid,))))

# ───────────── Reproducción ─────────────
@app.route('/api/v1/matings', methods=['POST'])
def add_mating():
    """Registra una monta"""
    data = request.json
    required = ['female_id', 'male_id', 'date']
    if not all(k in data for k in required):
        return jsonify({"error": f"Campos requeridos: {required}"}), 400
    
    parse_date(data['date'])
    
    # Validar que los conejos existan y sean del género correcto
    db = get_db()
    female = db.execute("SELECT gender FROM rabbits WHERE id=?", (data['female_id'],)).fetchone()
    male = db.execute("SELECT gender FROM rabbits WHERE id=?", (data['male_id'],)).fetchone()
    
    if not female or female['gender'] != 'female':
        return jsonify({"error": "female_id debe ser una hembra válida"}), 400
    if not male or male['gender'] != 'male':
        return jsonify({"error": "male_id debe ser un macho válido"}), 400
    
    db.execute("""
        INSERT INTO mating_logs (female_id, male_id, date, effective, notes) 
        VALUES (?,?,?,?,?)
    """, (data['female_id'], data['male_id'], data['date'], 
          int(data.get('effective', 0)), data.get('notes')))
        mid = db.execute("SELECT last_insert_rowid()").fetchone()[0]
    db.commit()
    
    # Generar alerta de posible parto si fue efectiva
    if data.get('effective'):
        settings = {r['key']: r['value'] for r in db.execute("SELECT key, value FROM settings")}
        gestation = int(settings.get("gestation_days", 31))
        due_date = parse_date(data['date']) + timedelta(days=gestation)
        
        db.execute("""
            INSERT INTO alerts (rabbit_id, alert_type, title, message, priority, metadata)
            VALUES (?, 'gestation_due', ?, ?, 'high', ?)
        """, (
            data['female_id'],
            f"Parto estimado: {female['gender']}",
            f"Fecha estimada: {due_date.strftime('%d/%m/%Y')}",
            json.dumps({"due_date": due_date.isoformat(), "mating_id": mid})
        ))
        db.commit()
    
    return jsonify({"id": mid, "message": "Monta registrada"}), 201

@app.route('/api/v1/litters', methods=['POST'])
def add_litter():
    """Registra un parto vinculado a una monta"""
    data = request.json
    required = ['mating_id', 'birth_date', 'total_born', 'live_born']
    if not all(k in data for k in required):
        return jsonify({"error": f"Campos requeridos: {required}"}), 400
    
    parse_date(data['birth_date'])
    
    db = get_db()
    # Verificar que la monta existe y es efectiva
    mating = db.execute(
        "SELECT female_id, effective FROM mating_logs WHERE id=?", (data['mating_id'],)
    ).fetchone()
    
    if not mating:
        return jsonify({"error": "Monta no encontrada"}), 404
    if not mating['effective'] and data.get('total_born', 0) > 0:
        # Advertencia: parto sin monta efectiva registrada
        pass  # Permitir pero registrar en logs
    
    db.execute("""
        INSERT INTO litter_logs (
            mating_id, birth_date, total_born, live_born, dead_born, 
            weaned_count, notes
        ) VALUES (?,?,?,?,?,?,?)
    """, (        data['mating_id'], data['birth_date'], data['total_born'],
        data['live_born'], data.get('dead_born', 0),
        data.get('weaned_count', 0), data.get('notes')
    ))
    db.commit()
    
    # Generar alerta de destete
    settings = {r['key']: r['value'] for r in db.execute("SELECT key, value FROM settings")}
    weaning_days = int(settings.get("weaning_age_days", 30))
    wean_date = parse_date(data['birth_date']) + timedelta(days=weaning_days)
    
    db.execute("""
        INSERT INTO alerts (rabbit_id, alert_type, title, message, priority, metadata)
        VALUES (?, 'weaning_due', ?, ?, 'medium', ?)
    """, (
        mating['female_id'],
        "Destete próximo",
        f"Crías listas para destete el {wean_date.strftime('%d/%m')}",
        json.dumps({"weaning_date": wean_date.isoformat(), "litter_born": data['live_born']})
    ))
    db.commit()
    
    return jsonify({"message": "Parto registrado"}), 201

# ───────────── Alertas ─────────────
def calculate_alerts():
    """Motor de generación de alertas basado en reglas configurables"""
    db = get_db()
    settings = {r['key']: r['value'] for r in db.execute("SELECT key, value FROM settings")}
    now = date.today()
    alerts = []
    buffer = int(settings.get("alert_days_before", 3))
    
    # 1. Edad reproductiva próxima
    f_thresh = int(settings.get("female_reproductive_age_months", 5)) * 30
    m_thresh = int(settings.get("male_reproductive_age_months", 6)) * 30
    
    for r in db.execute("""
        SELECT id, name, gender, birth_date FROM rabbits 
        WHERE birth_date IS NOT NULL AND status='alive' AND cage_id IS NOT NULL
    """):
        birth = parse_date(r['birth_date'])
        age_days = (now - birth).days
        target = f_thresh if r['gender'] == 'female' else m_thresh
        days_left = target - age_days
        
        if 0 <= days_left <= buffer:
            alerts.append({
                "rabbit_id": r['id'],
                "type": "reproductive_age",                "title": f"{'Hembra' if r['gender']=='female' else 'Macho'} en edad reproductiva",
                "message": f"{r['name']} alcanza edad reproductiva en {days_left} días",
                "priority": "high" if days_left <= 1 else "medium",
                "metadata": {"target_date": (birth + timedelta(days=target)).isoformat()}
            })
    
    # 2. Ciclos de celo (hembras con historial de monta)
    cycle_days = 28
    for mating in db.execute("""
        SELECT m.id, m.date, m.female_id, r.name as female_name 
        FROM mating_logs m 
        JOIN rabbits r ON m.female_id = r.id 
        WHERE r.status='alive' AND r.gender='female'
        ORDER BY m.date DESC
    """):
        last_mating = parse_date(mating['date'])
        next_heat = last_mating + timedelta(days=cycle_days)
        days_until = (next_heat - now).days
        
        if 0 <= days_until <= buffer:
            # Evitar duplicados: verificar si ya hay alerta similar no reconocida
            exists = db.execute("""
                SELECT 1 FROM alerts 
                WHERE rabbit_id=? AND alert_type='heat_cycle' 
                AND acknowledged=0 AND triggered_at > datetime('now', '-7 days')
            """, (mating['female_id'],)).fetchone()
            
            if not exists:
                alerts.append({
                    "rabbit_id": mating['female_id'],
                    "type": "heat_cycle",
                    "title": f"Celo próximo: {mating['female_name']}",
                    "message": f"Posible celo en {days_until} días",
                    "priority": "high",
                    "metadata": {
                        "predicted_heat_date": next_heat.isoformat(),
                        "last_mating_id": mating['id']
                    }
                })
    
    # 3. Partos próximos (gestación)
    gestation = int(settings.get("gestation_days", 31))
    for mating in db.execute("""
        SELECT m.*, r.name as female_name 
        FROM mating_logs m 
        JOIN rabbits r ON m.female_id = r.id 
        WHERE m.effective=1 AND r.status='alive'
    """):
        mating_date = parse_date(mating['date'])
        due_date = mating_date + timedelta(days=gestation)        days_until = (due_date - now).days
        
        if 0 <= days_until <= buffer:
            has_litter = db.execute(
                "SELECT 1 FROM litter_logs WHERE mating_id=?", (mating['id'],)
            ).fetchone()
            if not has_litter:
                alerts.append({
                    "rabbit_id": mating['female_id'],
                    "type": "gestation_due",
                    "title": f"Parto próximo: {mating['female_name']}",
                    "message": f"Fecha estimada: {due_date.strftime('%d/%m')}",
                    "priority": "high",
                    "metadata": {
                        "due_date": due_date.isoformat(),
                        "mating_id": mating['id']
                    }
                })
    
    # 4. Destete de crías
    weaning_days = int(settings.get("weaning_age_days", 30))
    for litter in db.execute("""
        SELECT l.*, m.female_id, r.name as mother_name
        FROM litter_logs l
        JOIN mating_logs m ON l.mating_id = m.id
        JOIN rabbits r ON m.female_id = r.id
        WHERE l.birth_date IS NOT NULL
    """):
        birth = parse_date(litter['birth_date'])
        wean_date = birth + timedelta(days=weaning_days)
        days_until = (wean_date - now).days
        
        if 0 <= days_until <= buffer:
            alerts.append({
                "rabbit_id": litter['female_id'],
                "type": "weaning_due",
                "title": f"Destete próximo: camada de {litter['mother_name']}",
                "message": f"Crías listas para destete en {days_until} días",
                "priority": "medium",
                "metadata": {
                    "weaning_date": wean_date.isoformat(),
                    "litter_id": litter['id'],
                    "live_born": litter['live_born']
                }
            })
    
    return alerts

@app.route('/api/v1/alerts', methods=['GET'])
def get_alerts():    """Obtiene alertas activas (calculadas + persistentes)"""
    db = get_db()
    
    # Alertas calculadas en tiempo real
    calculated = calculate_alerts()
    
    # Alertas persistentes no reconocidas
    persistent = rows_to_list(db.execute("""
        SELECT a.*, r.name as rabbit_name 
        FROM alerts a 
        LEFT JOIN rabbits r ON a.rabbit_id = r.id
        WHERE a.acknowledged=0 
        ORDER BY 
            CASE a.priority WHEN 'high' THEN 1 WHEN 'medium' THEN 2 ELSE 3 END,
            a.triggered_at DESC
    """))
    
    # Combinar y deduplicar por tipo+rabbit_id+fecha
    seen = set()
    unique_alerts = []
    
    for alert in persistent + calculated:
        key = (alert.get('rabbit_id'), alert.get('type'), alert.get('metadata', '{}'))
        if key not in seen:
            seen.add(key)
            unique_alerts.append(alert)
    
    return jsonify({
        "alerts": unique_alerts,
        "count": len(unique_alerts),
        "generated_at": datetime.now().isoformat(),
        "notification_window": is_notification_time()
    })

@app.route('/api/v1/alerts/<alert_id>/acknowledge', methods=['POST'])
def acknowledge_alert(alert_id):
    """Marca una alerta como reconocida"""
    db = get_db()
    db.execute("""
        UPDATE alerts SET acknowledged=1, resolved_at=datetime('now') WHERE id=?
    """, (alert_id,))
    db.commit()
    return jsonify({"message": "Alerta reconocida"})

# ───────────── Estadísticas (TABLAS, SIN GRÁFICOS) ─────────────
@app.route('/api/v1/stats/<scope>', methods=['GET'])
def get_stats(scope):
    """Estadísticas en formato tabular (sin gráficos)"""
    db = get_db()
        if scope == 'general':
        # Resumen general de la granja
        stats = {
            "total_rabbits": db.execute("SELECT COUNT(*) FROM rabbits").fetchone()[0],
            "alive": db.execute("SELECT COUNT(*) FROM rabbits WHERE status='alive'").fetchone()[0],
            "by_gender": {},
            "by_status": {},
            "by_breed": {},
            "reproduction": {
                "total_matings": db.execute("SELECT COUNT(*) FROM mating_logs").fetchone()[0],
                "effective_matings": db.execute("SELECT COUNT(*) FROM mating_logs WHERE effective=1").fetchone()[0],
                "total_litters": db.execute("SELECT COUNT(*) FROM litter_logs").fetchone()[0],
                "total_born": db.execute("SELECT COALESCE(SUM(total_born),0) FROM litter_logs").fetchone()[0],
                "total_weaned": db.execute("SELECT COALESCE(SUM(weaned_count),0) FROM litter_logs").fetchone()[0],
            },
            "cages": {
                "total": db.execute("SELECT COUNT(*) FROM cages WHERE is_active=1").fetchone()[0],
                "occupied": db.execute("""
                    SELECT COUNT(DISTINCT cage_id) FROM rabbits 
                    WHERE cage_id IS NOT NULL AND status='alive'
                """).fetchone()[0]
            }
        }
        
        # Desgloses
        for gender in ('male', 'female', 'unknown'):
            stats['by_gender'][gender] = db.execute(
                "SELECT COUNT(*) FROM rabbits WHERE gender=? AND status='alive'", (gender,)
            ).fetchone()[0]
        
        for status in ('alive', 'dead', 'sold', 'retired'):
            stats['by_status'][status] = db.execute(
                "SELECT COUNT(*) FROM rabbits WHERE status=?", (status,)
            ).fetchone()[0]
        
        for breed in db.execute("SELECT DISTINCT breed FROM rabbits"):
            stats['by_breed'][breed['breed']] = db.execute(
                "SELECT COUNT(*) FROM rabbits WHERE breed=? AND status='alive'", (breed['breed'],)
            ).fetchone()[0]
        
        return jsonify(stats)
    
    elif scope == 'rabbit':
        rid = request.args.get('id')
        if not rid:
            return jsonify({"error": "Parámetro requerido: ?id="}), 400
        
        rabbit = row_to_dict(db.execute("SELECT * FROM rabbits WHERE id=?", (rid,)).fetchone())
        if not rabbit:
            return jsonify({"error": "Conejo no encontrado"}), 404        
        # Historial de peso
        weights = rows_to_list(db.execute("""
            SELECT date, weight_lb FROM weight_logs 
            WHERE rabbit_id=? ORDER BY date
        """, (rid,)))
        
        # Estadísticas de peso
        weight_stats = {}
        if weights:
            weight_values = [w['weight_lb'] for w in weights]
            weight_stats = {
                "current": weight_values[-1],
                "min": min(weight_values),
                "max": max(weight_values),
                "avg": round(sum(weight_values) / len(weight_values), 2),
                "trend": "up" if len(weight_values) >= 2 and weight_values[-1] > weight_values[-2] else 
                         "down" if len(weight_values) >= 2 else "stable"
            }
        
        # Reproducción
        if rabbit['gender'] == 'female':
            matings = rows_to_list(db.execute(
                "SELECT * FROM mating_logs WHERE female_id=? ORDER BY date DESC", (rid,)
            ))
            litters = rows_to_list(db.execute("""
                SELECT l.* FROM litter_logs l
                JOIN mating_logs m ON l.mating_id = m.id
                WHERE m.female_id=? ORDER BY l.birth_date DESC
            """, (rid,)))
            repro_stats = {
                "total_matings": len(matings),
                "effective_matings": sum(1 for m in matings if m['effective']),
                "total_litters": len(litters),
                "total_born": sum(l['total_born'] for l in litters),
                "total_weaned": sum(l['weaned_count'] for l in litters),
                "success_rate": round(
                    sum(l['weaned_count'] for l in litters) / 
                    max(1, sum(l['live_born'] for l in litters)) * 100, 1
                ) if litters else 0
            }
        else:
            matings = rows_to_list(db.execute(
                "SELECT * FROM mating_logs WHERE male_id=? ORDER BY date DESC", (rid,)
            ))
            repro_stats = {
                "total_matings": len(matings),
                "effective_matings": sum(1 for m in matings if m['effective']),
                "offspring_count": db.execute("""
                    SELECT COUNT(*) FROM rabbits                     WHERE sire_id=? AND status='alive'
                """, (rid,)).fetchone()[0]
            }
        
        return jsonify({
            "rabbit": rabbit,
            "weight_history": weights,
            "weight_stats": weight_stats,
            "reproduction": repro_stats
        })
    
    elif scope == 'cage':
        cid = request.args.get('id')
        if not cid:
            return jsonify({"error": "Parámetro requerido: ?id="}), 400
        
        cage = row_to_dict(db.execute("SELECT * FROM cages WHERE id=?", (cid,)).fetchone())
        if not cage:
            return jsonify({"error": "Jaula no encontrada"}), 404
        
        rabbits = rows_to_list(db.execute("""
            SELECT id, name, gender, breed, status, birth_date 
            FROM rabbits WHERE cage_id=? ORDER BY name
        """, (cid,)))
        
        return jsonify({
            "cage": cage,
            "rabbits": rabbits,
            "occupancy": {
                "current": len([r for r in rabbits if r['status'] == 'alive']),
                "capacity": cage['capacity'],
                "available": cage['capacity'] - len([r for r in rabbits if r['status'] == 'alive'])
            }
        })
    
    return jsonify({"error": "Scope inválido. Use: general, rabbit, cage"}), 400

# ───────────── Configuración ─────────────
@app.route('/api/v1/settings', methods=['GET', 'POST'])
def settings():
    """CRUD de configuración global"""
    db = get_db()
    
    if request.method == 'GET':
        return jsonify({r['key']: r['value'] for r in db.execute("SELECT * FROM settings")})
    
    data = request.json
    if not isinstance(data, dict):
        return jsonify({"error": "Datos deben ser un objeto JSON"}), 400
        for k, v in data.items():
        if k in DEFAULT_SETTINGS:  # Solo permitir keys conocidas
            db.execute("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", (k, str(v)))
    
    db.commit()
    return jsonify({"message": "Configuración actualizada", "updated_keys": list(data.keys())})

# ───────────── Exportar / Importar BD ─────────────
@app.route('/api/v1/export', methods=['GET'])
def export_db():
    """Exporta la BD completa o en formato JSON estructurado"""
    db = get_db()
    db.commit()  # Asegurar checkpoint WAL
    
    # Opción JSON para interoperabilidad
    if request.args.get('format') == 'json':
        export = {
            "meta": {
                "app": "Cunnis",
                "version": "0.1.0.0",
                "package": "cu.dandroid.cunnis",
                "exported_at": datetime.now().isoformat(),
                "schema_version": 1
            },
            "settings": {r['key']: r['value'] for r in db.execute("SELECT * FROM settings")},
            "cages": rows_to_list(db.execute("SELECT * FROM cages")),
            "rabbits": rows_to_list(db.execute("SELECT * FROM rabbits")),
            "weight_logs": rows_to_list(db.execute("SELECT * FROM weight_logs")),
            "health_logs": rows_to_list(db.execute("SELECT * FROM health_logs")),
            "mating_logs": rows_to_list(db.execute("SELECT * FROM mating_logs")),
            "litter_logs": rows_to_list(db.execute("SELECT * FROM litter_logs")),
            "alerts": rows_to_list(db.execute("SELECT * FROM alerts"))
        }
        return jsonify(export)
    
    # Opción binaria: archivo .cunnisdb para restauración directa
    return send_file(
        DB_PATH, 
        as_attachment=True, 
        download_name=f"cunnis_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.cunnisdb",
        mimetype='application/x-sqlite3'
    )

@app.route('/api/v1/import', methods=['POST'])
def import_db():
    """Restaura la BD desde un backup"""
    if 'file' not in request.files:
        return jsonify({"error": "No se proporcionó archivo"}), 400
    
    f = request.files['file']    if not f.filename.endswith(('.db', '.cunnisdb', '.sqlite', '.json')):
        return jsonify({"error": "Extensión no soportada"}), 400
    
    # Guardar temporalmente
    temp_path = os.path.join(BACKUP_DIR, secure_filename(f"import_{uuid.uuid4().hex}_{f.filename}"))
    f.save(temp_path)
    
    db = get_db()
    
    try:
        if f.filename.endswith('.json'):
            # Importar desde JSON (merge parcial)
            with open(temp_path, 'r', encoding='utf-8') as import_file:
                data = json.load(import_file)
            
            # Restaurar settings
            for k, v in data.get('settings', {}).items():
                db.execute("INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)", (k, str(v)))
            
            # Nota: Para datos completos se requeriría lógica de merge más compleja
            db.commit()
            return jsonify({"message": "Configuración importada desde JSON"})
        
        else:
            # Reemplazo completo de BD
            # 1. Backup automático de la BD actual
            if os.path.exists(DB_PATH):
                auto_backup = os.path.join(
                    BACKUP_DIR, 
                    f"auto_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
                )
                shutil.copy2(DB_PATH, auto_backup)
            
            # 2. Reemplazar con el archivo importado
            shutil.copy2(temp_path, DB_PATH)
            
            # 3. Re-inicializar conexión
            close_db(None)
            get_db()  # Nueva conexión
            
            return jsonify({
                "message": "Base de datos restaurada exitosamente",
                "backup_created": auto_backup if os.path.exists(auto_backup) else None
            })
    
    except Exception as e:
        return jsonify({"error": f"Error durante la importación: {str(e)}"}), 500
    
    finally:
        if os.path.exists(temp_path):            os.remove(temp_path)

# ───────────── Búsqueda Avanzada ─────────────
@app.route('/api/v1/search', methods=['GET'])
def search_rabbits():
    """Búsqueda full-text simulada con filtros combinados"""
    query = request.args.get('q', '').strip().lower()
    
    if not query:
        return jsonify({"error": "Parámetro de búsqueda requerido: ?q="}), 400
    
    # Búsqueda en múltiples campos
    results = rows_to_list(get_db().execute("""
        SELECT DISTINCT r.* FROM rabbits r
        WHERE status='alive' AND (
            LOWER(r.name) LIKE ? OR 
            LOWER(r.breed) LIKE ? OR 
            LOWER(r.id) LIKE ? OR
            r.id IN (
                SELECT sire_id FROM rabbits WHERE LOWER(name) LIKE ?
            ) OR
            r.id IN (
                SELECT dam_id FROM rabbits WHERE LOWER(name) LIKE ?
            )
        )
        ORDER BY r.name
    """, [f"%{query}%"] * 5))
    
    return jsonify({
        "query": query,
        "results": results,
        "count": len(results)
    })

# ─────────────────────────────────────────────────────────────────────────────
# INICIALIZACIÓN
# ─────────────────────────────────────────────────────────────────────────────
if __name__ == '__main__':
    # Crear icono de prueba si no existe
    if not os.path.exists(os.path.join(ICONS_DIR, "ic_launcher.png")):
        # Placeholder: en producción, el frontend debe proveer este archivo
        print("⚠️  ic_launcher.png no encontrado. Cree assets/icons/ic_launcher.png")
    
    init_db()
    print(f"🐇 Cunnis Backend v0.1.0.0 iniciado")
    print(f"📦 Package: cu.dandroid.cunnis")
    print(f"🔗 API disponible en http://localhost:5000/api/v1")
    print(f"👤 Autor: Dannel Alvarez Nieto | @alvareznieto.dannel")
    print(f"💙 Dedicatoria: a mi viejo")
        # En producción: usar gunicorn, desactivar debug
    app.run(host='0.0.0.0', port=5000, debug=True)