// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "attributefactory.h"
#include "integerbase.h"
#include "floatbase.h"
#include "flagattribute.h"
#include "defines.h"

#include <vespa/log/log.h>
LOG_SETUP(".createarrayfastsearch");

#include "attributevector.hpp"
#include "enumstore.hpp"
#include "enumattribute.hpp"
#include "multivalueattribute.hpp"
#include "multienumattribute.hpp"
#include "multinumericenumattribute.hpp"
#include "multinumericpostattribute.hpp"
#include "multistringpostattribute.hpp"

namespace search {

using attribute::BasicType;

#define INTARRAY(T)   MultiValueNumericPostingAttribute< ENUM_ATTRIBUTE(IntegerAttributeTemplate<T>), MULTIVALUE_ENUM_ARG >
#define FLOATARRAY(T) MultiValueNumericPostingAttribute< ENUM_ATTRIBUTE(FloatingPointAttributeTemplate<T>), MULTIVALUE_ENUM_ARG >
#define CREATEINTARRAY(T, fname, info) static_cast<AttributeVector *>(new INTARRAY(T)(fname, info))
#define CREATEFLOATARRAY(T, fname, info) static_cast<AttributeVector *>(new FLOATARRAY(T)(fname, info))

AttributeVector::SP
AttributeFactory::createArrayFastSearch(const vespalib::string & baseFileName, const Config & info)
{
    assert(info.collectionType().type() == attribute::CollectionType::ARRAY);
    assert(info.fastSearch());
    AttributeVector::SP ret;
    switch(info.basicType().type()) {
    case BasicType::UINT1:
    case BasicType::UINT2:
    case BasicType::UINT4:
        break;
    case BasicType::INT8:
        ret.reset(static_cast<AttributeVector *>(new FlagAttribute(baseFileName, info)));
        break;
    case BasicType::INT16:
        ret.reset(CREATEINTARRAY(int16_t, baseFileName, info));
        break;
    case BasicType::INT32:
        ret.reset(CREATEINTARRAY(int32_t, baseFileName, info));
        break;
    case BasicType::INT64:
        ret.reset(CREATEINTARRAY(int64_t, baseFileName, info));
        break;
    case BasicType::FLOAT:
        ret.reset(CREATEFLOATARRAY(float, baseFileName, info));
        break;
    case BasicType::DOUBLE:
        ret.reset(CREATEFLOATARRAY(double, baseFileName, info));
        break;
    case BasicType::STRING:
        ret.reset(static_cast<AttributeVector *>(new ArrayStringPostingAttribute(baseFileName, info)));
        break;
    default:
        break;
    }
    return ret;
}

}
