//
// Created by Ankio on 2024/7/12.
//

#ifndef AUTOACCOUNTING_AUTHMODEL_H
#define AUTOACCOUNTING_AUTHMODEL_H
#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(AuthModel, "auth")
        FIELD_PK_AI(int, id)
        FIELD(std::string, app)
        FIELD(std::string, token)
END_TABLE()
#endif //AUTOACCOUNTING_AUTHMODEL_H
