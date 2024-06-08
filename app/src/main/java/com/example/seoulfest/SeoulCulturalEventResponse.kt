package com.example.seoulfest.models

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "culturalEventInfo", strict = false)
data class SeoulCulturalEventResponse(
    @field:ElementList(name = "row", inline = true)
    var events: List<CulturalEvent>? = null
)

@Root(name = "row", strict = false)
data class CulturalEvent(
    @field:Element(name = "CODENAME", required = false)
    var codename: String? = null,

    @field:Element(name = "GUNAME", required = false)
    var guname: String? = null,

    @field:Element(name = "TITLE", required = false)
    var title: String? = null,

    @field:Element(name = "DATE", required = false)
    var date: String? = null,

    @field:Element(name = "PLACE", required = false)
    var place: String? = null,

    @field:Element(name = "USE_FEE", required = false)
    var useFee: String? = null,

    @field:Element(name = "MAIN_IMG", required = false)
    var mainImg: String? = null,

    @field:Element(name = "LAT", required = false)
    var lat: String? = null,

    @field:Element(name = "LOT", required = false)
    var lng: String? = null

)
