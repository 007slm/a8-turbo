/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { AxiosResponse } from "axios";
import axios from "axios";
import { cancelToken } from "@/utils/cancelToken";

interface SkywalkingResponse {
    errors?: any;
    [key: string]: any;
}

async function query(param: { queryStr: string; conditions: { [key: string]: unknown } }): Promise<any> {
    const res: AxiosResponse<any> = await axios.post(
        "/skywalking-oap/graphql",
        { query: param.queryStr, variables: { ...param.conditions } },
        { cancelToken: cancelToken() },
    );
    const data = res.data;
    if (data.errors) {
        data.errors = data.errors.map((e: { message: string }) => e.message).join(" ");
    }
    return data as any;
}

export default query;
