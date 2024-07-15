// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_RULEMODEL_H
#define AUTOACCOUNTING_RULEMODEL_H

#include <string>
#include "db/orm/Utils.h"


/**
 * 规则进行拆分，支持根据不同渠道进行规则匹配
 */

DEFINE_TABLE(RuleModel, "rule")
        FIELD_PK_AI(int, id)
        FIELD(std::string, app)
        FIELD(int, type)
        FIELD(int, use) //是否启用
        FIELD(int, auto_record) //是否自动记账
        FIELD(std::string, name) //规则名称是所有规则的唯一ID，不可重复
END_TABLE()

#endif //AUTOACCOUNTING_RULEMODEL_H
