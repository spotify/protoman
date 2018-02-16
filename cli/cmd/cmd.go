/*
Copyright 2018 Spotify AB. All rights reserved.

The contents of this file are licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cmd

import (
	"errors"
	"fmt"
	"os"

	"github.com/spf13/cobra"
	"github.com/spotify/protoman/cli/publish"
	"github.com/spotify/protoman/cli/validator"
)

var rootCmd = &cobra.Command{
	Use:   "protoman",
	Short: "Protoman is a fantastic way of managing your protos",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("PROTOMAN PROTOMAN!")
		fmt.Println("You probably need help, call me with -h")
	},
}

func init() {
	rootCmd.AddCommand(versionCmd)
	rootCmd.AddCommand(validateCmd)
	rootCmd.AddCommand(publishCmd)
	rootCmd.PersistentFlags().StringP("server", "s", "", "Protoman server address")
}

var versionCmd = &cobra.Command{
	Use:   "version",
	Short: "Print the version number of protoman",
	Run: func(cmd *cobra.Command, args []string) {
		fmt.Println("Protoman version 0.0.1")
	},
}

var validateCmd = &cobra.Command{
	Use:   "validate [path]",
	Short: "Validate proto defintion file(s)",
	Args: func(cmd *cobra.Command, args []string) error {
		if len(args) < 1 {
			return errors.New("Path required")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		if _, err := os.Stat(args[0]); err == nil {
			if err := validator.Validate(args[0]); err != nil {
				fmt.Printf("Error: %v\n", err)
				os.Exit(1)
			}
		}
	},
}

var publishCmd = &cobra.Command{
	Use:   "publish [path]",
	Short: "Publish proto defintion file(s) to the protoman registry",
	Args: func(cmd *cobra.Command, args []string) error {
		if len(args) < 1 {
			return errors.New("Path required")
		}
		if cmd.Flag("server").Value.String() == "" {
			return errors.New("--server must be specified when publishing")
		}
		return nil
	},
	Run: func(cmd *cobra.Command, args []string) {
		if _, err := os.Stat(args[0]); err == nil {
			if err := publish.Publish(args[0], cmd.Flag("server").Value.String()); err != nil {
				fmt.Printf("Error: %v\n", err)
				os.Exit(1)
			}
		}
	},
}

// Execute CLI
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
