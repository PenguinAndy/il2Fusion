#include "db.h"

#include <atomic>
#include <mutex>
#include "sqlite3.h"
#include <string>
#include <unistd.h>

#include "log.h"

namespace textdb {

namespace {
std::string g_db_path;
sqlite3* g_db = nullptr;
std::mutex g_db_mutex;
std::atomic_bool g_db_logged{false};
std::string g_process_name;

std::string BuildDbPath(const std::string& process_name) {
    std::string pkg = process_name;
    const auto pos = pkg.find(':');
    if (pos != std::string::npos) {
        pkg = pkg.substr(0, pos);
    }
    if (pkg.empty()) {
        pkg = "unknown";
    }
    return "/data/data/" + pkg + "/text.db";
}

bool EnsureDatabaseLocked(bool log_path) {
    if (g_db != nullptr) {
        if (log_path && !g_db_logged.load()) {
            LOGI("数据库路径: %s", g_db_path.c_str());
            g_db_logged.store(true);
        }
        return true;
    }

    const bool existed = (access(g_db_path.c_str(), F_OK) == 0);

    int rc = sqlite3_open(g_db_path.c_str(), &g_db);
    if (rc != SQLITE_OK) {
        LOGE("打开数据库失败 %s, rc=%d", g_db_path.c_str(), rc);
        g_db = nullptr;
        return false;
    }

    const char* create_sql =
            "CREATE TABLE IF NOT EXISTS text_log ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "txt TEXT UNIQUE"
            ");";
    rc = sqlite3_exec(g_db, create_sql, nullptr, nullptr, nullptr);
    if (rc != SQLITE_OK) {
        LOGE("创建表失败 rc=%d", rc);
        sqlite3_close(g_db);
        g_db = nullptr;
        return false;
    }

    if (log_path) {
        LOGI("数据库路径: %s (%s)", g_db_path.c_str(), existed ? "已存在" : "新建");
        g_db_logged.store(true);
    }
    return true;
}

bool TextExistsLocked(const std::string& text) {
    const char* sql = "SELECT 1 FROM text_log WHERE txt = ? LIMIT 1;";
    sqlite3_stmt* stmt = nullptr;
    if (sqlite3_prepare_v2(g_db, sql, -1, &stmt, nullptr) != SQLITE_OK) {
        return false;
    }
    sqlite3_bind_text(stmt, 1, text.c_str(), -1, SQLITE_TRANSIENT);
    const int rc = sqlite3_step(stmt);
    const bool exists = (rc == SQLITE_ROW);
    sqlite3_finalize(stmt);
    return exists;
}

}  // namespace

void Init(const std::string& process_name, bool log_path) {
    std::lock_guard<std::mutex> _lk(g_db_mutex);
    g_process_name = process_name;
    g_db_path = BuildDbPath(process_name);
    EnsureDatabaseLocked(log_path);
}

void InsertIfNeeded(const std::string& text) {
    if (text.empty()) {
        LOGI("跳过插入：文本为空");
        return;
    }

    std::lock_guard<std::mutex> _lk(g_db_mutex);
    if (g_db_path.empty()) {
        g_db_path = BuildDbPath(g_process_name);
    }
    if (!EnsureDatabaseLocked(false)) {
        LOGE("跳过插入：数据库未就绪");
        return;
    }

    if (TextExistsLocked(text)) {
        LOGI("跳过插入：已存在 -> %s", text.c_str());
        return;
    }

    const char* sql = "INSERT INTO text_log(txt) VALUES (?);";
    sqlite3_stmt* stmt = nullptr;
    if (sqlite3_prepare_v2(g_db, sql, -1, &stmt, nullptr) != SQLITE_OK) {
        LOGE("插入预编译失败");
        return;
    }
    sqlite3_bind_text(stmt, 1, text.c_str(), -1, SQLITE_TRANSIENT);
    const int rc = sqlite3_step(stmt);
    sqlite3_finalize(stmt);
    if (rc == SQLITE_DONE) {
        LOGI("数据库新增文本: %s", text.c_str());
    } else {
        LOGE("插入失败 rc=%d 文本=%s", rc, text.c_str());
    }
}

}  // namespace textdb
