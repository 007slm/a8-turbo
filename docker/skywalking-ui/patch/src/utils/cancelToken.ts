import axios from "axios";

export function cancelToken() {
    if (typeof axios.CancelToken?.source === 'function') {
        return axios.CancelToken.source().token;
    }
    return undefined;
}
