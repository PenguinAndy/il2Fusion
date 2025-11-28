#pragma once
#include "logging.h"

#define CHECK(cond) do { if (!(cond)) { ERROR_LOG("CHECK failed: %s", #cond); abort(); } } while (0)
#define CHECK_EQ(a,b) CHECK((a) == (b))
#define CHECK_NE(a,b) CHECK((a) != (b))
#define CHECK_LT(a,b) CHECK((a) < (b))
#define CHECK_LE(a,b) CHECK((a) <= (b))
#define CHECK_GT(a,b) CHECK((a) > (b))
#define CHECK_GE(a,b) CHECK((a) >= (b))

#ifdef NDEBUG
#define DCHECK(cond) do { (void)sizeof(cond); } while (0)
#define DCHECK_EQ(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#define DCHECK_NE(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#define DCHECK_LT(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#define DCHECK_LE(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#define DCHECK_GT(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#define DCHECK_GE(a,b) do { (void)sizeof(a); (void)sizeof(b); } while (0)
#else
#define DCHECK(cond) CHECK(cond)
#define DCHECK_EQ(a,b) CHECK_EQ(a,b)
#define DCHECK_NE(a,b) CHECK_NE(a,b)
#define DCHECK_LT(a,b) CHECK_LT(a,b)
#define DCHECK_LE(a,b) CHECK_LE(a,b)
#define DCHECK_GT(a,b) CHECK_GT(a,b)
#define DCHECK_GE(a,b) CHECK_GE(a,b)
#endif
