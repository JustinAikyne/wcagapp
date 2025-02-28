package com.brahos.accessibilitychecker.model;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

@Data
@Document(collection = "wcag_fixes")
public class WcagFixesData {

    @Id
    @Field("_id")
    private ObjectId id;
    private String wcagVersion;
    private String level;
    private Map<String, IssueFixDetails> data;

}
