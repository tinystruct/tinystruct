/*******************************************************************************
 * Copyright  (c) 2013, 2017 James Mover Zhou
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.transfer.http;

import org.tinystruct.ApplicationException;
import org.tinystruct.transfer.Reception;
import org.tinystruct.transfer.Transmission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class Receptor implements Reception {

    private HttpURLConnection connection;

    public void accept(Transmission transmission) throws ApplicationException {
        this.connection = (HttpURLConnection) transmission.getConnection();

        try {
            // Get the response code
            int responseCode = this.connection.getResponseCode();
            // Check if the request was successful
            if (responseCode == 200) {
                // Read the response data
                transmission.stop();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        this.connection.getInputStream()));

                String line;
                StringBuffer buffer = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    buffer.append("\n").append(line);
                }
                in.close();
                this.stop();
            } else {
                System.out.println("Request failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e.getCause());
        }
    }

    public void start() {

    }

    public void stop() {
        try {
            this.connection.getInputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
